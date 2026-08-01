package com.noblesi.travelplanner.dto.plan;

import com.noblesi.travelplanner.domain.plan.PlanVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateTravelPlanMetadataRequest(
		@NotBlank(message = "플랜 제목은 필수 값입니다.")
		@Size(max = 200, message = "플랜 제목은 200자 이하여야 합니다.")
		String title,

		@NotNull(message = "공개 범위는 필수 값입니다.")
		PlanVisibility visibility,

		@NotNull(message = "플랜 버전은 필수 값입니다.")
		@PositiveOrZero(message = "플랜 버전은 0 이상이어야 합니다.")
		Integer versionNo
) {

	public UpdateTravelPlanMetadataRequest {
		title = title == null ? null : title.strip();
	}
}
