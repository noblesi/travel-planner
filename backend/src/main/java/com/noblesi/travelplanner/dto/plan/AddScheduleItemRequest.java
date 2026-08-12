package com.noblesi.travelplanner.dto.plan;

import java.math.BigDecimal;

import com.noblesi.travelplanner.domain.plan.TimeSlot;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AddScheduleItemRequest(
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

		@NotBlank(message = "장소 제공자는 필수 값입니다.")
		@Pattern(regexp = "TOUR_API", message = "지원하지 않는 장소 제공자입니다.")
		String placeProvider,

		@NotBlank(message = "외부 장소 ID는 필수 값입니다.")
		@Size(max = 100, message = "외부 장소 ID는 100자 이하여야 합니다.")
		String externalPlaceId,

		@Size(max = 200, message = "장소명은 200자 이하여야 합니다.")
		String placeName,

		@Size(max = 100, message = "카테고리는 100자 이하여야 합니다.")
		String categoryName,

		@Size(max = 500, message = "주소는 500자 이하여야 합니다.")
		String address,

		@DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
		@DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
		BigDecimal latitude,

		@DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
		@DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
		BigDecimal longitude,

		@Size(max = 1000, message = "이미지 URL은 1000자 이하여야 합니다.")
		String imageUrl,

		@Size(max = 4000, message = "설명은 4000자 이하여야 합니다.")
		String description
) {
}
