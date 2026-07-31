package com.noblesi.travelplanner.dto.plan;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateTravelPlanDatesRequest(
		@NotNull(message = "여행 시작일은 필수 값입니다.")
		LocalDate startDate,

		@NotNull(message = "여행 종료일은 필수 값입니다.")
		LocalDate endDate,

		@NotNull(message = "플랜 버전은 필수 값입니다.")
		@PositiveOrZero(message = "플랜 버전은 0 이상이어야 합니다.")
		Integer versionNo,

		boolean force
) {
}
