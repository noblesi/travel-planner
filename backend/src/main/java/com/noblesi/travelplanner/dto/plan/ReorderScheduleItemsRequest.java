package com.noblesi.travelplanner.dto.plan;

import java.util.List;

import com.noblesi.travelplanner.domain.plan.TimeSlot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ReorderScheduleItemsRequest(
		@NotBlank(message = "작업 ID는 필수 값입니다.")
		@Pattern(
				regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
				message = "작업 ID는 UUID 형식이어야 합니다."
		)
		String operationId,

		@NotNull(message = "일정 버전은 필수 값입니다.")
		@PositiveOrZero(message = "일정 버전은 0 이상이어야 합니다.")
		Integer scheduleVersion,

		@NotNull(message = "일정 시간대는 필수 값입니다.")
		TimeSlot timeSlot,

		@NotNull(message = "정렬할 일정 ID 목록은 필수 값입니다.")
		@Size(max = 100, message = "시간대별 일정은 최대 100개까지 지원합니다.")
		@Valid
		List<@NotBlank(message = "일정 ID는 빈 값일 수 없습니다.") String> scheduleItemIds
) {
}
