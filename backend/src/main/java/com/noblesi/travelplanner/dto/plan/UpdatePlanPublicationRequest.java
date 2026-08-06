package com.noblesi.travelplanner.dto.plan;

import com.noblesi.travelplanner.domain.plan.PlanPublishStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdatePlanPublicationRequest(
		@NotNull(message = "발행 상태는 필수 값입니다.")
		PlanPublishStatus publishStatus,

		@NotNull(message = "플랜 버전은 필수 값입니다.")
		@PositiveOrZero(message = "플랜 버전은 0 이상이어야 합니다.")
		Integer versionNo
) {
}
