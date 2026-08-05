package com.noblesi.travelplanner.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.TimeSlot;
import com.noblesi.travelplanner.mapper.PlanDayMapper;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;

@Component
class PlanScheduleMutationSupport {

	static final int MAX_ITEMS_PER_TIME_SLOT = 100;
	static final int REORDER_TEMPORARY_POSITION_OFFSET = 1000;

	private final PlanDayMapper planDayMapper;
	private final PlanScheduleItemMapper planScheduleItemMapper;

	PlanScheduleMutationSupport(
			PlanDayMapper planDayMapper,
			PlanScheduleItemMapper planScheduleItemMapper
	) {
		this.planDayMapper = planDayMapper;
		this.planScheduleItemMapper = planScheduleItemMapper;
	}

	PlanDay requireOwnedDay(long planDayId, long planId) {
		PlanDay day = planDayMapper.findByIdAndPlanId(planDayId, planId);
		if (day == null) {
			throw new BusinessException(HttpStatus.NOT_FOUND, "PLAN_DAY_NOT_FOUND", "여행 일차를 찾을 수 없습니다.");
		}
		return day;
	}

	PlanScheduleItem requireScheduleItem(long scheduleItemId, long planDayId) {
		PlanScheduleItem item = planScheduleItemMapper.findByIdAndDayId(scheduleItemId, planDayId);
		if (item == null) {
			throw new BusinessException(HttpStatus.NOT_FOUND, "SCHEDULE_ITEM_NOT_FOUND", "일정 항목을 찾을 수 없습니다.");
		}
		return item;
	}

	void requireScheduleVersion(PlanDay day, int scheduleVersion) {
		if (day.scheduleVersion() != scheduleVersion) {
			throw scheduleVersionConflict();
		}
	}

	void requireItemVersion(PlanScheduleItem item, int itemVersion) {
		if (item.itemVersion() != itemVersion) {
			throw itemVersionConflict();
		}
	}

	void incrementScheduleVersion(long planDayId, long planId, int scheduleVersion) {
		if (planDayMapper.incrementScheduleVersion(planDayId, planId, scheduleVersion) != 1) {
			throw scheduleVersionConflict();
		}
	}

	void requireNoDuplicatePlace(
			long planDayId,
			TimeSlot timeSlot,
			String placeProvider,
			String externalPlaceId,
			Long excludedScheduleItemId
	) {
		if (planScheduleItemMapper.countDuplicatePlaceInSlot(
				planDayId,
				timeSlot,
				placeProvider,
				externalPlaceId,
				excludedScheduleItemId
		) > 0) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					"SCHEDULE_ITEM_ALREADY_EXISTS",
					"선택한 시간대에 같은 장소가 이미 있습니다."
			);
		}
	}

	List<Long> parseScheduleItemIds(List<String> values) {
		List<Long> ids;
		try {
			ids = values.stream()
					.map(value -> {
						if (!value.matches("[1-9]\\d*")) {
							throw new NumberFormatException();
						}
						return Long.parseLong(value);
					})
					.toList();
		} catch (NumberFormatException exception) {
			throw invalidScheduleOrder();
		}
		Set<Long> distinctIds = new HashSet<>(ids);
		if (distinctIds.size() != ids.size()) {
			throw invalidScheduleOrder();
		}
		return ids;
	}

	void requireExactOrderMembers(List<Long> currentItemIds, List<Long> requestedItemIds) {
		if (currentItemIds.size() != requestedItemIds.size()
				|| !new HashSet<>(currentItemIds).equals(new HashSet<>(requestedItemIds))) {
			throw invalidScheduleOrder();
		}
	}

	BusinessException scheduleVersionConflict() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"SCHEDULE_VERSION_CONFLICT",
				"다른 일정 변경이 먼저 저장되었습니다. 최신 일정을 다시 불러와 주세요."
		);
	}

	BusinessException itemVersionConflict() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"ITEM_VERSION_CONFLICT",
				"일정 항목이 이미 변경되었습니다. 최신 일정을 다시 불러와 주세요."
		);
	}

	BusinessException scheduleItemLimitExceeded() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"SCHEDULE_ITEM_LIMIT_EXCEEDED",
				"시간대별 일정은 최대 100개까지 추가할 수 있습니다."
		);
	}

	String normalizeNullable(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	void requireSingleRow(int affectedRows) {
		if (affectedRows != 1) {
			throw new IllegalStateException("Expected one affected row but got " + affectedRows);
		}
	}

	private BusinessException invalidScheduleOrder() {
		return new BusinessException(
				HttpStatus.BAD_REQUEST,
				"INVALID_SCHEDULE_ORDER",
				"정렬 목록은 선택한 시간대의 모든 일정 항목을 중복 없이 포함해야 합니다."
		);
	}

}
