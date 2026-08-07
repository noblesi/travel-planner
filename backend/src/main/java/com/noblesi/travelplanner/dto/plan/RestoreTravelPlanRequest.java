package com.noblesi.travelplanner.dto.plan;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RestoreTravelPlanRequest(
		@NotNull(message = "플랜 버전은 필수 값입니다.")
		@PositiveOrZero(message = "플랜 버전은 0 이상이어야 합니다.")
		Integer versionNo
) {
}
