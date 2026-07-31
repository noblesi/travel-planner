package com.noblesi.travelplanner.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.domain.plan.TimeSlot;
import com.noblesi.travelplanner.dto.plan.AddScheduleItemRequest;
import com.noblesi.travelplanner.dto.plan.DeleteScheduleItemRequest;
import com.noblesi.travelplanner.dto.plan.ReorderScheduleItemsRequest;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;
import com.noblesi.travelplanner.dto.plan.UpdateScheduleItemRequest;
import com.noblesi.travelplanner.mapper.PlanDayMapper;
import com.noblesi.travelplanner.mapper.PlanEditOperationMapper;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;
import com.noblesi.travelplanner.mapper.TravelPlanMapper;
import com.noblesi.travelplanner.security.CurrentMemberProvider;

@Service
public class PlanScheduleService {

	private static final int MAX_ITEMS_PER_TIME_SLOT = 100;
	private static final int REORDER_TEMPORARY_POSITION_OFFSET = 1000;

	private final CurrentMemberProvider currentMemberProvider;
	private final TravelPlanMapper travelPlanMapper;
	private final PlanDayMapper planDayMapper;
	private final PlanScheduleItemMapper planScheduleItemMapper;
	private final PlanEditOperationMapper planEditOperationMapper;
	private final TravelPlanService travelPlanService;

	public PlanScheduleService(
			CurrentMemberProvider currentMemberProvider,
			TravelPlanMapper travelPlanMapper,
			PlanDayMapper planDayMapper,
			PlanScheduleItemMapper planScheduleItemMapper,
			PlanEditOperationMapper planEditOperationMapper,
			TravelPlanService travelPlanService
	) {
		this.currentMemberProvider = currentMemberProvider;
		this.travelPlanMapper = travelPlanMapper;
		this.planDayMapper = planDayMapper;
		this.planScheduleItemMapper = planScheduleItemMapper;
		this.planEditOperationMapper = planEditOperationMapper;
		this.travelPlanService = travelPlanService;
	}

	@Transactional
	public ScheduleMutationResponse addScheduleItem(
			String planIdValue,
			String planDayIdValue,
			AddScheduleItemRequest request
	) {
		long planId = parsePositiveId(planIdValue, "planId");
		long planDayId = parsePositiveId(planDayIdValue, "dayId");
		long memberId = requireOwnedPlan(planId);
		String operationId = normalizeOperationId(request.operationId());
		String requestHash = requestHash(
				request.scheduleVersion(),
				request.timeSlot(),
				request.placeProvider(),
				request.externalPlaceId().trim(),
				request.placeName().trim(),
				normalizeNullable(request.categoryName()),
				normalizeNullable(request.address()),
				request.latitude(),
				request.longitude(),
				normalizeNullable(request.imageUrl()),
				normalizeNullable(request.description())
		);

		ScheduleMutationResponse replay = replayIfProcessed(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.ADD,
				request.scheduleVersion(),
				requestHash,
				planIdValue
		);
		if (replay != null) {
			return replay;
		}

		PlanDay day = requireOwnedDay(planDayId, planId);
		requireScheduleVersion(day, request.scheduleVersion());
		int itemCount = planScheduleItemMapper.countByDayAndTimeSlot(
				planDayId,
				request.timeSlot()
		);
		if (itemCount >= MAX_ITEMS_PER_TIME_SLOT) {
			throw scheduleItemLimitExceeded();
		}
		requireNoDuplicatePlace(
				planDayId,
				request.timeSlot(),
				request.placeProvider(),
				request.externalPlaceId().trim(),
				null
		);

		incrementScheduleVersion(planDayId, planId, request.scheduleVersion());
		long scheduleItemId = planScheduleItemMapper.nextScheduleItemId();
		PlanScheduleItem item = new PlanScheduleItem(
				scheduleItemId,
				planDayId,
				request.timeSlot(),
				itemCount + 1,
				request.placeProvider(),
				request.externalPlaceId().trim(),
				request.placeName().trim(),
				normalizeNullable(request.categoryName()),
				normalizeNullable(request.address()),
				request.latitude(),
				request.longitude(),
				normalizeNullable(request.imageUrl()),
				normalizeNullable(request.description()),
				0
		);
		requireSingleRow(planScheduleItemMapper.insertScheduleItem(item));

		int resultVersion = request.scheduleVersion() + 1;
		insertOperation(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.ADD,
				scheduleItemId,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return response(operationId, scheduleItemId, resultVersion, planIdValue);
	}

	@Transactional
	public ScheduleMutationResponse updateScheduleItem(
			String planIdValue,
			String planDayIdValue,
			String scheduleItemIdValue,
			UpdateScheduleItemRequest request
	) {
		long planId = parsePositiveId(planIdValue, "planId");
		long planDayId = parsePositiveId(planDayIdValue, "dayId");
		long scheduleItemId = parsePositiveId(scheduleItemIdValue, "itemId");
		long memberId = requireOwnedPlan(planId);
		String operationId = normalizeOperationId(request.operationId());
		String requestHash = requestHash(
				scheduleItemId,
				request.scheduleVersion(),
				request.itemVersion(),
				request.timeSlot()
		);

		ScheduleMutationResponse replay = replayIfProcessed(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.UPDATE,
				request.scheduleVersion(),
				requestHash,
				planIdValue
		);
		if (replay != null) {
			return replay;
		}

		PlanDay day = requireOwnedDay(planDayId, planId);
		PlanScheduleItem item = requireScheduleItem(scheduleItemId, planDayId);
		requireItemVersion(item, request.itemVersion());
		requireScheduleVersion(day, request.scheduleVersion());

		if (item.timeSlot() == request.timeSlot()) {
			insertOperation(new PlanEditOperation(
					operationId,
					planId,
					memberId,
					ScheduleOperationType.UPDATE,
					scheduleItemId,
					request.scheduleVersion(),
					request.scheduleVersion(),
					requestHash
			));
			return response(
					operationId,
					scheduleItemId,
					request.scheduleVersion(),
					planIdValue
			);
		}

		int targetCount = planScheduleItemMapper.countByDayAndTimeSlot(
				planDayId,
				request.timeSlot()
		);
		if (targetCount >= MAX_ITEMS_PER_TIME_SLOT) {
			throw scheduleItemLimitExceeded();
		}
		requireNoDuplicatePlace(
				planDayId,
				request.timeSlot(),
				item.placeProvider(),
				item.externalPlaceId(),
				scheduleItemId
		);

		incrementScheduleVersion(planDayId, planId, request.scheduleVersion());
		int updatedRows = planScheduleItemMapper.updateTimeSlot(
				scheduleItemId,
				planDayId,
				request.timeSlot(),
				targetCount + 1,
				request.itemVersion()
		);
		if (updatedRows != 1) {
			throw itemVersionConflict();
		}
		planScheduleItemMapper.compactPositions(
				planDayId,
				item.timeSlot(),
				item.positionNo()
		);

		int resultVersion = request.scheduleVersion() + 1;
		insertOperation(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.UPDATE,
				scheduleItemId,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return response(operationId, scheduleItemId, resultVersion, planIdValue);
	}

	@Transactional
	public ScheduleMutationResponse deleteScheduleItem(
			String planIdValue,
			String planDayIdValue,
			String scheduleItemIdValue,
			DeleteScheduleItemRequest request
	) {
		long planId = parsePositiveId(planIdValue, "planId");
		long planDayId = parsePositiveId(planDayIdValue, "dayId");
		long scheduleItemId = parsePositiveId(scheduleItemIdValue, "itemId");
		long memberId = requireOwnedPlan(planId);
		String operationId = normalizeOperationId(request.operationId());
		String requestHash = requestHash(
				scheduleItemId,
				request.scheduleVersion(),
				request.itemVersion()
		);

		ScheduleMutationResponse replay = replayIfProcessed(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.DELETE,
				request.scheduleVersion(),
				requestHash,
				planIdValue
		);
		if (replay != null) {
			return replay;
		}

		PlanDay day = requireOwnedDay(planDayId, planId);
		PlanScheduleItem item = requireScheduleItem(scheduleItemId, planDayId);
		requireItemVersion(item, request.itemVersion());
		requireScheduleVersion(day, request.scheduleVersion());
		incrementScheduleVersion(planDayId, planId, request.scheduleVersion());

		int deletedRows = planScheduleItemMapper.deleteByIdAndVersion(
				scheduleItemId,
				planDayId,
				request.itemVersion()
		);
		if (deletedRows != 1) {
			throw itemVersionConflict();
		}
		planScheduleItemMapper.compactPositions(
				planDayId,
				item.timeSlot(),
				item.positionNo()
		);

		int resultVersion = request.scheduleVersion() + 1;
		insertOperation(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.DELETE,
				scheduleItemId,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return response(operationId, scheduleItemId, resultVersion, planIdValue);
	}

	@Transactional
	public ScheduleMutationResponse reorderScheduleItems(
			String planIdValue,
			String planDayIdValue,
			ReorderScheduleItemsRequest request
	) {
		long planId = parsePositiveId(planIdValue, "planId");
		long planDayId = parsePositiveId(planDayIdValue, "dayId");
		long memberId = requireOwnedPlan(planId);
		List<Long> requestedItemIds = parseScheduleItemIds(request.scheduleItemIds());
		String operationId = normalizeOperationId(request.operationId());
		String requestHash = requestHash(
				request.scheduleVersion(),
				request.timeSlot(),
				requestedItemIds
		);

		ScheduleMutationResponse replay = replayIfProcessed(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.REORDER,
				request.scheduleVersion(),
				requestHash,
				planIdValue
		);
		if (replay != null) {
			return replay;
		}

		PlanDay day = requireOwnedDay(planDayId, planId);
		requireScheduleVersion(day, request.scheduleVersion());
		List<Long> currentItemIds = planScheduleItemMapper.findIdsByDayAndTimeSlot(
				planDayId,
				request.timeSlot()
		);
		requireExactOrderMembers(currentItemIds, requestedItemIds);

		int resultVersion = request.scheduleVersion();
		if (!currentItemIds.equals(requestedItemIds)) {
			incrementScheduleVersion(planDayId, planId, request.scheduleVersion());
			planScheduleItemMapper.movePositionsToTemporaryRange(
					planDayId,
					request.timeSlot(),
					REORDER_TEMPORARY_POSITION_OFFSET
			);
			int positionNo = 1;
			for (long scheduleItemId : requestedItemIds) {
				requireSingleRow(planScheduleItemMapper.updatePosition(
						scheduleItemId,
						planDayId,
						request.timeSlot(),
						positionNo
				));
				positionNo++;
			}
			resultVersion++;
		}

		insertOperation(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.REORDER,
				null,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return response(operationId, null, resultVersion, planIdValue);
	}

	private long requireOwnedPlan(long planId) {
		long memberId = currentMemberProvider.getCurrentMemberId();
		if (travelPlanMapper.findActiveOwnedPlanForEditor(planId, memberId) == null) {
			throw new BusinessException(
					HttpStatus.NOT_FOUND,
					"PLAN_NOT_FOUND",
					"여행 플랜을 찾을 수 없습니다."
			);
		}
		return memberId;
	}

	private PlanDay requireOwnedDay(long planDayId, long planId) {
		PlanDay day = planDayMapper.findByIdAndPlanId(planDayId, planId);
		if (day == null) {
			throw new BusinessException(
					HttpStatus.NOT_FOUND,
					"PLAN_DAY_NOT_FOUND",
					"여행 일차를 찾을 수 없습니다."
			);
		}
		return day;
	}

	private PlanScheduleItem requireScheduleItem(long scheduleItemId, long planDayId) {
		PlanScheduleItem item = planScheduleItemMapper.findByIdAndDayId(
				scheduleItemId,
				planDayId
		);
		if (item == null) {
			throw new BusinessException(
					HttpStatus.NOT_FOUND,
					"SCHEDULE_ITEM_NOT_FOUND",
					"일정 항목을 찾을 수 없습니다."
			);
		}
		return item;
	}

	private void requireScheduleVersion(PlanDay day, int scheduleVersion) {
		if (day.scheduleVersion() != scheduleVersion) {
			throw scheduleVersionConflict();
		}
	}

	private void requireItemVersion(PlanScheduleItem item, int itemVersion) {
		if (item.itemVersion() != itemVersion) {
			throw itemVersionConflict();
		}
	}

	private void incrementScheduleVersion(long planDayId, long planId, int scheduleVersion) {
		if (planDayMapper.incrementScheduleVersion(planDayId, planId, scheduleVersion) != 1) {
			throw scheduleVersionConflict();
		}
	}

	private void requireNoDuplicatePlace(
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

	private ScheduleMutationResponse replayIfProcessed(
			String operationId,
			long planId,
			long memberId,
			ScheduleOperationType operationType,
			int baseVersion,
			String requestHash,
			String planIdValue
	) {
		PlanEditOperation existing = planEditOperationMapper.findByOperationId(operationId);
		if (existing == null) {
			return null;
		}
		if (existing.planId() != planId
				|| existing.memberId() != memberId
				|| existing.operationType() != operationType
				|| existing.baseVersion() != baseVersion
				|| !existing.requestHash().equals(requestHash)) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					"DUPLICATE_OPERATION",
					"같은 작업 ID가 다른 요청에 이미 사용되었습니다."
			);
		}
		return response(
				existing.operationId(),
				existing.targetItemId(),
				existing.resultVersion(),
				planIdValue
		);
	}

	private void insertOperation(PlanEditOperation operation) {
		requireSingleRow(planEditOperationMapper.insertOperation(operation));
	}

	private ScheduleMutationResponse response(
			String operationId,
			Long scheduleItemId,
			int resultVersion,
			String planIdValue
	) {
		return ScheduleMutationResponse.of(
				operationId,
				scheduleItemId,
				resultVersion,
				travelPlanService.getPlanEditor(planIdValue)
		);
	}

	private List<Long> parseScheduleItemIds(List<String> values) {
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

	private void requireExactOrderMembers(List<Long> currentItemIds, List<Long> requestedItemIds) {
		if (currentItemIds.size() != requestedItemIds.size()
				|| !new HashSet<>(currentItemIds).equals(new HashSet<>(requestedItemIds))) {
			throw invalidScheduleOrder();
		}
	}

	private BusinessException invalidScheduleOrder() {
		return new BusinessException(
				HttpStatus.BAD_REQUEST,
				"INVALID_SCHEDULE_ORDER",
				"정렬 목록은 선택한 시간대의 모든 일정 항목을 중복 없이 포함해야 합니다."
		);
	}

	private BusinessException scheduleVersionConflict() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"SCHEDULE_VERSION_CONFLICT",
				"다른 일정 변경이 먼저 저장되었습니다. 최신 일정을 다시 불러와 주세요."
		);
	}

	private BusinessException itemVersionConflict() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"ITEM_VERSION_CONFLICT",
				"일정 항목이 이미 변경되었습니다. 최신 일정을 다시 불러와 주세요."
		);
	}

	private BusinessException scheduleItemLimitExceeded() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"SCHEDULE_ITEM_LIMIT_EXCEEDED",
				"시간대별 일정은 최대 100개까지 추가할 수 있습니다."
		);
	}

	private long parsePositiveId(String value, String parameterName) {
		if (value == null || !value.matches("[1-9]\\d*")) {
			throw invalidPathParameter(parameterName);
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException exception) {
			throw invalidPathParameter(parameterName);
		}
	}

	private BusinessException invalidPathParameter(String parameterName) {
		return new BusinessException(
				HttpStatus.BAD_REQUEST,
				"INVALID_PATH_PARAMETER",
				parameterName + "는 1 이상의 숫자여야 합니다."
		);
	}

	private String normalizeOperationId(String operationId) {
		return operationId.toLowerCase(Locale.ROOT);
	}

	private String normalizeNullable(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String requestHash(Object... values) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			for (Object value : values) {
				String canonicalValue;
				if (value instanceof List<?> list) {
					canonicalValue = list.stream()
							.map(String::valueOf)
							.collect(java.util.stream.Collectors.joining(","));
				} else if (value instanceof BigDecimal decimal) {
					canonicalValue = decimal.stripTrailingZeros().toPlainString();
				} else {
					canonicalValue = Objects.toString(value, "<null>");
				}
				byte[] bytes = canonicalValue.getBytes(StandardCharsets.UTF_8);
				digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
				digest.update((byte) ':');
				digest.update(bytes);
				digest.update((byte) ';');
			}
			return java.util.HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private void requireSingleRow(int affectedRows) {
		if (affectedRows != 1) {
			throw new IllegalStateException("Expected one affected row but got " + affectedRows);
		}
	}
}
