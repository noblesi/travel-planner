package com.noblesi.travelplanner.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.dto.plan.PlanEditorDayResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorItemResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorSummaryResponse;
import com.noblesi.travelplanner.mapper.PlanDayMapper;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;

@Service
class PlanEditorQueryService {

	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final PlanDayMapper planDayMapper;
	private final PlanScheduleItemMapper planScheduleItemMapper;

	PlanEditorQueryService(
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			PlanDayMapper planDayMapper,
			PlanScheduleItemMapper planScheduleItemMapper
	) {
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.planDayMapper = planDayMapper;
		this.planScheduleItemMapper = planScheduleItemMapper;
	}

	@Transactional(readOnly = true)
	PlanEditorResponse getPlanEditor(String planIdValue) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		PlanEditorPlan plan = planAccessService.requireAccessiblePlan(planId, memberId);
		return buildResponse(planId, plan);
	}

	PlanEditorResponse buildResponse(long planId, PlanEditorPlan plan) {
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
}
