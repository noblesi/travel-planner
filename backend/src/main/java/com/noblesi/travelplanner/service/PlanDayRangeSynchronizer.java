package com.noblesi.travelplanner.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.TravelPlan;
import com.noblesi.travelplanner.mapper.PlanDayMapper;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;

@Component
class PlanDayRangeSynchronizer {

	private final PlanDayMapper planDayMapper;
	private final PlanScheduleItemMapper planScheduleItemMapper;

	PlanDayRangeSynchronizer(
			PlanDayMapper planDayMapper,
			PlanScheduleItemMapper planScheduleItemMapper
	) {
		this.planDayMapper = planDayMapper;
		this.planScheduleItemMapper = planScheduleItemMapper;
	}

	List<PlanDay> createPlanDays(TravelPlan travelPlan) {
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

	void synchronize(
			long planId,
			List<PlanDay> existingDays,
			LocalDate oldStartDate,
			LocalDate oldEndDate,
			LocalDate newStartDate,
			LocalDate newEndDate,
			boolean force
	) {
		long oldDuration = ChronoUnit.DAYS.between(oldStartDate, oldEndDate) + 1;
		long newDuration = ChronoUnit.DAYS.between(newStartDate, newEndDate) + 1;
		List<PlanDay> removedDays = oldDuration == newDuration
				? List.of()
				: existingDays.stream()
						.filter(day -> day.travelDate().isBefore(newStartDate)
								|| day.travelDate().isAfter(newEndDate))
						.toList();

		requireScheduleRemovalConfirmation(planId, removedDays, force);
		if (oldDuration == newDuration) {
			shiftPlanDays(existingDays, ChronoUnit.DAYS.between(oldStartDate, newStartDate));
			return;
		}
		reshapePlanDays(planId, existingDays, removedDays, newStartDate, newEndDate);
	}

	List<PlanDay> lockPlanDays(long planId) {
		return planDayMapper.findByPlanIdOrderByDayNoForUpdate(planId);
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

		Set<LocalDate> retainedDates = retainedDays.stream()
				.map(PlanDay::travelDate)
				.collect(Collectors.toSet());
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

	private void requireSingleRow(int affectedRows) {
		if (affectedRows != 1) {
			throw new IllegalStateException("Expected one affected row but got " + affectedRows);
		}
	}
}
