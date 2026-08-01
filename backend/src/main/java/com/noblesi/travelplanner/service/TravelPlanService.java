package com.noblesi.travelplanner.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanDatesRequest;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanMetadataRequest;
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
		PlanEditorPlan plan = findOwnedPlanForEditor(planId, memberId);

		return buildPlanEditorResponse(planId, plan);
	}

	@Transactional
	public PlanEditorResponse updateTravelPlanMetadata(
			String planIdValue,
			UpdateTravelPlanMetadataRequest request
	) {
		long planId = parsePlanId(planIdValue);
		long memberId = currentMemberProvider.getCurrentMemberId();
		PlanEditorPlan plan = findOwnedPlanForEditor(planId, memberId);

		if (request.versionNo() != plan.versionNo()) {
			throw planVersionConflict();
		}

		if (plan.title().equals(request.title())
				&& plan.visibility() == request.visibility()) {
			return buildPlanEditorResponse(planId, plan);
		}

		int updatedRows = travelPlanMapper.updateTravelPlanMetadata(
				planId,
				memberId,
				request.title(),
				request.visibility(),
				request.versionNo()
		);
		if (updatedRows != 1) {
			throw planVersionConflict();
		}

		return buildPlanEditorResponse(planId, findOwnedPlanForEditor(planId, memberId));
	}

	@Transactional
	public PlanEditorResponse updateTravelPlanDates(
			String planIdValue,
			UpdateTravelPlanDatesRequest request
	) {
		long planId = parsePlanId(planIdValue);
		long memberId = currentMemberProvider.getCurrentMemberId();
		PlanEditorPlan plan = findOwnedPlanForEditor(planId, memberId);
		requestValidator.validateDates(request.startDate(), request.endDate());

		if (request.versionNo() != plan.versionNo()) {
			throw planVersionConflict();
		}

		if (plan.startDate().equals(request.startDate())
				&& plan.endDate().equals(request.endDate())) {
			return buildPlanEditorResponse(planId, plan);
		}

		List<PlanDay> existingDays = planDayMapper.findByPlanIdOrderByDayNo(planId);
		long oldDuration = ChronoUnit.DAYS.between(plan.startDate(), plan.endDate()) + 1;
		long newDuration = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;

		List<PlanDay> removedDays = oldDuration == newDuration
				? List.of()
				: existingDays.stream()
						.filter(day -> day.travelDate().isBefore(request.startDate())
								|| day.travelDate().isAfter(request.endDate()))
						.toList();
		requireScheduleRemovalConfirmation(planId, removedDays, request.force());

		int updatedRows = travelPlanMapper.updateTravelDates(
				planId,
				memberId,
				request.startDate(),
				request.endDate(),
				request.versionNo()
		);
		if (updatedRows != 1) {
			throw planVersionConflict();
		}

		if (oldDuration == newDuration) {
			shiftPlanDays(existingDays, ChronoUnit.DAYS.between(plan.startDate(), request.startDate()));
		} else {
			reshapePlanDays(planId, existingDays, removedDays, request.startDate(), request.endDate());
		}

		PlanEditorPlan updatedPlan = findOwnedPlanForEditor(planId, memberId);
		return buildPlanEditorResponse(planId, updatedPlan);
	}

	private PlanEditorResponse buildPlanEditorResponse(long planId, PlanEditorPlan plan) {
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

	private PlanEditorPlan findOwnedPlanForEditor(long planId, long memberId) {
		PlanEditorPlan plan = travelPlanMapper.findActiveOwnedPlanForEditor(planId, memberId);
		if (plan == null) {
			throw new BusinessException(
					HttpStatus.NOT_FOUND,
					"PLAN_NOT_FOUND",
					"여행 플랜을 찾을 수 없습니다."
			);
		}
		return plan;
	}

	private void requireScheduleRemovalConfirmation(
			long planId,
			List<PlanDay> removedDays,
			boolean force
	) {
		if (force || removedDays.isEmpty()) {
			return;
		}

		Set<Long> removedDayIds = removedDays.stream()
				.map(PlanDay::planDayId)
				.collect(Collectors.toSet());
		boolean hasSchedulesToRemove = planScheduleItemMapper.findByPlanIdForEditor(planId).stream()
				.anyMatch(item -> removedDayIds.contains(item.planDayId()));
		if (hasSchedulesToRemove) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					"PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED",
					"변경 범위에서 제외되는 날짜에 일정이 있습니다. 확인 후 다시 요청해 주세요."
			);
		}
	}

	private void shiftPlanDays(List<PlanDay> existingDays, long dayOffset) {
		if (dayOffset == 0) {
			return;
		}

		List<PlanDay> orderedDays = new ArrayList<>(existingDays);
		Comparator<PlanDay> byTravelDate = Comparator.comparing(PlanDay::travelDate);
		orderedDays.sort(dayOffset > 0 ? byTravelDate.reversed() : byTravelDate);
		for (PlanDay day : orderedDays) {
			requireSingleRow(planDayMapper.updateTravelDate(
					day.planDayId(),
					day.travelDate().plusDays(dayOffset)
			));
		}
	}

	private void reshapePlanDays(
			long planId,
			List<PlanDay> existingDays,
			List<PlanDay> removedDays,
			LocalDate startDate,
			LocalDate endDate
	) {
		Set<Long> removedDayIds = removedDays.stream()
				.map(PlanDay::planDayId)
				.collect(Collectors.toSet());
		if (!removedDayIds.isEmpty()) {
			List<Long> ids = List.copyOf(removedDayIds);
			planScheduleItemMapper.deleteByPlanDayIds(ids);
			int deletedDays = planDayMapper.deleteByPlanDayIds(ids);
			if (deletedDays != ids.size()) {
				throw new IllegalStateException(
						"Expected " + ids.size() + " deleted plan days but got " + deletedDays
				);
			}
		}

		List<PlanDay> retainedDays = existingDays.stream()
				.filter(day -> !removedDayIds.contains(day.planDayId()))
				.collect(Collectors.toCollection(ArrayList::new));
		long dayNoOffset = retainedDays.isEmpty()
				? 0
				: ChronoUnit.DAYS.between(startDate, retainedDays.get(0).travelDate()) + 1
						- retainedDays.get(0).dayNo();
		Comparator<PlanDay> byDayNo = Comparator.comparingInt(PlanDay::dayNo);
		retainedDays.sort(dayNoOffset > 0 ? byDayNo.reversed() : byDayNo);
		for (PlanDay day : retainedDays) {
			int newDayNo = Math.toIntExact(ChronoUnit.DAYS.between(startDate, day.travelDate()) + 1);
			if (newDayNo != day.dayNo()) {
				requireSingleRow(planDayMapper.updateDayNo(day.planDayId(), newDayNo));
			}
		}

		Set<LocalDate> retainedDates = new HashSet<>();
		for (PlanDay day : retainedDays) {
			retainedDates.add(day.travelDate());
		}

		LocalDate travelDate = startDate;
		int dayNo = 1;
		while (!travelDate.isAfter(endDate)) {
			if (!retainedDates.contains(travelDate)) {
				requireSingleRow(planDayMapper.insertPlanDay(new PlanDay(
						planDayMapper.nextPlanDayId(),
						planId,
						dayNo,
						travelDate
				)));
			}
			travelDate = travelDate.plusDays(1);
			dayNo++;
		}
	}

	private BusinessException planVersionConflict() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"PLAN_VERSION_CONFLICT",
				"다른 변경사항이 먼저 저장되었습니다. 플랜을 새로고침한 후 다시 시도해 주세요."
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
