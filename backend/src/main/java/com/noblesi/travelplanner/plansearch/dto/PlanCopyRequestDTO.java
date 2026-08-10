package com.noblesi.travelplanner.plansearch.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanCopyRequestDTO {

	// TRAVEL_PLAN.TITLE의 필수/최대 길이 제약을 API 경계에서 먼저 검증해 DB 오류를 사용자 오류로 반환한다.
	@NotBlank(message = "플랜 제목은 필수 값입니다.")
	@Size(max = 200, message = "플랜 제목은 200자 이하여야 합니다.")
	private String title;

	// 여행 기간 도메인 검증 전에 필수 날짜 누락을 명확한 Validation 오류로 처리한다.
	@NotNull(message = "여행 시작일은 필수 값입니다.")
	private LocalDate startDate;

	@NotNull(message = "여행 종료일은 필수 값입니다.")
	private LocalDate endDate;
}
