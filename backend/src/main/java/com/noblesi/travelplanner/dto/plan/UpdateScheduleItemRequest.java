package com.noblesi.travelplanner.dto.plan;

import com.noblesi.travelplanner.domain.plan.TimeSlot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateScheduleItemRequest(
		@NotBlank(message = "작업 ID는 필수 값입니다.")
		@Pattern(
				regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
				message = "작업 ID는 UUID 형식이어야 합니다."
		)
		String operationId,

		@NotNull(message = "일정 버전은 필수 값입니다.")
		@PositiveOrZero(message = "일정 버전은 0 이상이어야 합니다.")
		Integer scheduleVersion,

		@NotNull(message = "항목 버전은 필수 값입니다.")
		@PositiveOrZero(message = "항목 버전은 0 이상이어야 합니다.")
		Integer itemVersion,

		@NotNull(message = "일정 시간대는 필수 값입니다.")
		TimeSlot timeSlot
) {
}
