package com.noblesi.travelplanner.dto.plan;

import java.time.LocalDate;

import com.noblesi.travelplanner.domain.plan.PlanVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTravelPlanRequest(
		@NotBlank(message = "지역코드는 필수 값입니다.")
		@Size(max = 20, message = "지역코드는 20자 이하여야 합니다.")
		@Pattern(regexp = "\\S+", message = "지역코드에 공백을 포함할 수 없습니다.")
		String regionCode,

		@NotNull(message = "여행 시작일은 필수 값입니다.")
		LocalDate startDate,

		@NotNull(message = "여행 종료일은 필수 값입니다.")
		LocalDate endDate,

		@NotNull(message = "공개 범위는 필수 값입니다.")
		PlanVisibility visibility
) {
}
