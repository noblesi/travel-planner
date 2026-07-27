package com.noblesi.travelplanner.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.ParticipantType;
import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.TravelPlan;
import com.noblesi.travelplanner.domain.region.Region;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanRequest;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorDayResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorItemResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorSummaryResponse;
import com.noblesi.travelplanner.mapper.PlanDayMapper;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;
import com.noblesi.travelplanner.mapper.RegionMapper;
import com.noblesi.travelplanner.mapper.TravelPlanMapper;
import com.noblesi.travelplanner.security.CurrentMemberProvider;

@Service
public class TravelPlanService {

	private final CurrentMemberProvider currentMemberProvider;
	private final TravelPlanRequestValidator requestValidator;
	private final RegionMapper regionMapper;
	private final TravelPlanMapper travelPlanMapper;
	private final PlanDayMapper planDayMapper;
	private final PlanScheduleItemMapper planScheduleItemMapper;

	public TravelPlanService(
			CurrentMemberProvider currentMemberProvider,
			TravelPlanRequestValidator requestValidator,
			RegionMapper regionMapper,
			TravelPlanMapper travelPlanMapper,
			PlanDayMapper planDayMapper,
			PlanScheduleItemMapper planScheduleItemMapper
	) {
		this.currentMemberProvider = currentMemberProvider;
		this.requestValidator = requestValidator;
		this.regionMapper = regionMapper;
		this.travelPlanMapper = travelPlanMapper;
		this.planDayMapper = planDayMapper;
		this.planScheduleItemMapper = planScheduleItemMapper;
	}

	@Transactional
	public CreateTravelPlanResponse createTravelPlan(CreateTravelPlanRequest request) {
		long memberId = currentMemberProvider.getCurrentMemberId();
		Region region = findActiveSidoRegion(request.regionCode());
		requestValidator.validate(request);

		long planId = travelPlanMapper.nextTravelPlanId();
		TravelPlan travelPlan = new TravelPlan(
				planId,
				memberId,
				region.regionName() + " 여행",
				region.regionCode(),
				request.startDate(),
				request.endDate(),
				request.visibility()
		);

		requireSingleRow(travelPlanMapper.insertTravelPlan(travelPlan));
		requireSingleRow(travelPlanMapper.insertPlanMember(
				planId,
				memberId,
				ParticipantType.CREATOR
		));

		List<PlanDay> planDays = createPlanDays(travelPlan);
		return CreateTravelPlanResponse.of(travelPlan, region, planDays);
	}

	@Transactional(readOnly = true)
	public PlanEditorResponse getPlanEditor(String planIdValue) {
		long planId = parsePlanId(planIdValue);
		long memberId = currentMemberProvider.getCurrentMemberId();
		PlanEditorPlan plan = travelPlanMapper.findActiveOwnedPlanForEditor(planId, memberId);
		if (plan == null) {
			throw new BusinessException(
					HttpStatus.NOT_FOUND,
					"PLAN_NOT_FOUND",
					"여행 플랜을 찾을 수 없습니다."
			);
		}

		List<PlanDay> days = planDayMapper.findByPlanIdOrderByDayNo(planId);
		Map<Long, List<PlanEditorItemResponse>> itemsByDayId = groupItemsByDayId(
				planScheduleItemMapper.findByPlanIdForEditor(planId)
		);
		List<PlanEditorDayResponse> dayResponses = days.stream()
				.map(day -> PlanEditorDayResponse.of(
						day,
						itemsByDayId.getOrDefault(day.planDayId(), List.of())
				))
				.toList();

		return new PlanEditorResponse(
				PlanEditorSummaryResponse.from(plan),
				dayResponses
		);
	}

	private Region findActiveSidoRegion(String regionCode) {
		Region region = regionMapper.findActiveSidoRegionByCode(regionCode);
		if (region == null) {
			throw new BusinessException(
					HttpStatus.NOT_FOUND,
					"REGION_NOT_FOUND",
					"선택한 여행지역을 찾을 수 없습니다."
			);
		}
		return region;
	}

	private List<PlanDay> createPlanDays(TravelPlan travelPlan) {
		List<PlanDay> planDays = new ArrayList<>();
		LocalDate travelDate = travelPlan.startDate();
		int dayNo = 1;
		while (!travelDate.isAfter(travelPlan.endDate())) {
			PlanDay planDay = new PlanDay(
					planDayMapper.nextPlanDayId(),
					travelPlan.planId(),
					dayNo,
					travelDate
			);
			requireSingleRow(planDayMapper.insertPlanDay(planDay));
			planDays.add(planDay);
			travelDate = travelDate.plusDays(1);
			dayNo++;
		}
		return planDays;
	}

	private Map<Long, List<PlanEditorItemResponse>> groupItemsByDayId(
			List<PlanScheduleItem> items
	) {
		Map<Long, List<PlanEditorItemResponse>> itemsByDayId = new HashMap<>();
		for (PlanScheduleItem item : items) {
			itemsByDayId.computeIfAbsent(item.planDayId(), ignored -> new ArrayList<>())
					.add(PlanEditorItemResponse.from(item));
		}
		return itemsByDayId;
	}

	private long parsePlanId(String planIdValue) {
		if (planIdValue == null || !planIdValue.matches("[1-9]\\d*")) {
			throw invalidPlanId();
		}
		try {
			return Long.parseLong(planIdValue);
		} catch (NumberFormatException exception) {
			throw invalidPlanId();
		}
	}

	private BusinessException invalidPlanId() {
		return new BusinessException(
				HttpStatus.BAD_REQUEST,
				"INVALID_PATH_PARAMETER",
				"planId는 1 이상의 숫자여야 합니다."
		);
	}

	private void requireSingleRow(int affectedRows) {
		if (affectedRows != 1) {
			throw new IllegalStateException("Expected one affected row but got " + affectedRows);
		}
	}
}
