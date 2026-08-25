package com.noblesi.travelplanner.service;

import java.time.Clock;
import java.time.OffsetDateTime;
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
import com.noblesi.travelplanner.mapper.PlanMemberMapper;
import com.noblesi.travelplanner.mapper.RegionMapper;
import com.noblesi.travelplanner.persistence.jpa.plan.TravelPlanEntity;
import com.noblesi.travelplanner.persistence.jpa.plan.TravelPlanRepository;

@Service
class TravelPlanCreationService {

	private final PlanAccessService planAccessService;
	private final TravelPlanRequestValidator requestValidator;
	private final RegionMapper regionMapper;
	private final TravelPlanRepository travelPlanRepository;
	private final PlanMemberMapper planMemberMapper;
	private final PlanDayRangeSynchronizer dayRangeSynchronizer;
	private final Clock clock;

	TravelPlanCreationService(
			PlanAccessService planAccessService,
			TravelPlanRequestValidator requestValidator,
			RegionMapper regionMapper,
			TravelPlanRepository travelPlanRepository,
			PlanMemberMapper planMemberMapper,
			PlanDayRangeSynchronizer dayRangeSynchronizer,
			Clock clock
	) {
		this.planAccessService = planAccessService;
		this.requestValidator = requestValidator;
		this.regionMapper = regionMapper;
		this.travelPlanRepository = travelPlanRepository;
		this.planMemberMapper = planMemberMapper;
		this.dayRangeSynchronizer = dayRangeSynchronizer;
		this.clock = clock;
	}

	@Transactional
	CreateTravelPlanResponse create(CreateTravelPlanRequest request) {
		long memberId = planAccessService.currentMemberId();
		Region region = findActiveSidoRegion(request.regionCode());
		requestValidator.validate(request);

		TravelPlanEntity planEntity = TravelPlanEntity.create(
				memberId,
				region.regionName() + " 여행",
				region.regionCode(),
				request.startDate(),
				request.endDate(),
				request.visibility(),
				OffsetDateTime.now(clock)
		);
		travelPlanRepository.saveAndFlush(planEntity);
		TravelPlan travelPlan = planEntity.toDomain();
		long planId = travelPlan.planId();
		requireSingleRow(planMemberMapper.insertPlanMember(planId, memberId, ParticipantType.CREATOR));
		List<PlanDay> planDays = dayRangeSynchronizer.createPlanDays(travelPlan);
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

	private void requireSingleRow(int affectedRows) {
		if (affectedRows != 1) {
			throw new IllegalStateException("Expected one affected row but got " + affectedRows);
		}
	}
}
