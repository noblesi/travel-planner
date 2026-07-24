package com.noblesi.travelplanner.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.ParticipantType;
import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.TravelPlan;
import com.noblesi.travelplanner.domain.region.Region;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanRequest;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanResponse;
import com.noblesi.travelplanner.mapper.PlanDayMapper;
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

	public TravelPlanService(
			CurrentMemberProvider currentMemberProvider,
			TravelPlanRequestValidator requestValidator,
			RegionMapper regionMapper,
			TravelPlanMapper travelPlanMapper,
			PlanDayMapper planDayMapper
	) {
		this.currentMemberProvider = currentMemberProvider;
		this.requestValidator = requestValidator;
		this.regionMapper = regionMapper;
		this.travelPlanMapper = travelPlanMapper;
		this.planDayMapper = planDayMapper;
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

	private void requireSingleRow(int affectedRows) {
		if (affectedRows != 1) {
			throw new IllegalStateException("Expected one affected row but got " + affectedRows);
		}
	}
}
