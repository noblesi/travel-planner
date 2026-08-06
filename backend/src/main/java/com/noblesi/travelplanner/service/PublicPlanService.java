package com.noblesi.travelplanner.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.PublicTravelPlan;
import com.noblesi.travelplanner.dto.plan.PublicPlanDayResponse;
import com.noblesi.travelplanner.dto.plan.PublicPlanDetailResponse;
import com.noblesi.travelplanner.dto.plan.PublicPlanItemResponse;
import com.noblesi.travelplanner.dto.plan.PublicPlanSearchResponse;
import com.noblesi.travelplanner.dto.plan.PublicPlanSummaryResponse;
import com.noblesi.travelplanner.mapper.PlanDayMapper;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;
import com.noblesi.travelplanner.mapper.PublicPlanMapper;

@Service
public class PublicPlanService {
	private static final int MAX_SEARCH_LIMIT = 100;

	private final PublicPlanMapper publicPlanMapper;
	private final PlanDayMapper planDayMapper;
	private final PlanScheduleItemMapper planScheduleItemMapper;

	public PublicPlanService(
			PublicPlanMapper publicPlanMapper,
			PlanDayMapper planDayMapper,
			PlanScheduleItemMapper planScheduleItemMapper
	) {
		this.publicPlanMapper = publicPlanMapper;
		this.planDayMapper = planDayMapper;
		this.planScheduleItemMapper = planScheduleItemMapper;
	}

	@Transactional(readOnly = true)
	public PublicPlanSearchResponse search(String keywordValue, int requestedPage, int requestedSize) {
		String keyword = keywordValue == null ? "" : keywordValue.strip();
		int page = Math.max(requestedPage, 1);
		int size = Math.min(Math.max(requestedSize, 1), MAX_SEARCH_LIMIT);
		long offset = (long) (page - 1) * size;
		int totalCount = publicPlanMapper.countPublicPlans(keyword);
		int totalPages = totalCount == 0
				? 0
				: (int) ((totalCount + (long) size - 1) / size);
		List<PublicPlanSummaryResponse> plans = publicPlanMapper.findPublicPlans(keyword, offset, size)
				.stream()
				.map(PublicPlanSummaryResponse::from)
				.toList();
		return new PublicPlanSearchResponse(
				keyword,
				page,
				size,
				totalCount,
				totalPages,
				page < totalPages,
				plans
		);
	}

	@Transactional
	public PublicPlanDetailResponse getDetail(String planIdValue) {
		long planId = parsePlanId(planIdValue);
		if (publicPlanMapper.incrementPublicPlanViewCount(planId) != 1) {
			throw planNotFound();
		}
		PublicTravelPlan plan = publicPlanMapper.findPublicPlanById(planId);
		if (plan == null) {
			throw planNotFound();
		}

		List<PlanDay> days = planDayMapper.findByPlanIdOrderByDayNo(planId);
		Map<Long, List<PublicPlanItemResponse>> itemsByDayId = new HashMap<>();
		for (PlanScheduleItem item : planScheduleItemMapper.findByPlanIdForEditor(planId)) {
			itemsByDayId.computeIfAbsent(item.planDayId(), ignored -> new ArrayList<>())
					.add(PublicPlanItemResponse.from(item));
		}
		List<PublicPlanDayResponse> dayResponses = days.stream()
				.map(day -> PublicPlanDayResponse.of(
						day,
						itemsByDayId.getOrDefault(day.planDayId(), List.of())
				))
				.toList();
		return new PublicPlanDetailResponse(PublicPlanSummaryResponse.from(plan), dayResponses);
	}

	private long parsePlanId(String planIdValue) {
		if (planIdValue == null || !planIdValue.matches("[1-9]\\d*")) {
			throw invalidPlanId("planId는 1 이상의 숫자여야 합니다.");
		}
		try {
			return Long.parseLong(planIdValue);
		} catch (NumberFormatException exception) {
			throw invalidPlanId("planId는 64비트 정수 범위여야 합니다.");
		}
	}

	private BusinessException invalidPlanId(String message) {
		return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PATH_PARAMETER", message);
	}

	private BusinessException planNotFound() {
		return new BusinessException(
				HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "공개 여행 플랜을 찾을 수 없습니다."
		);
	}
}
