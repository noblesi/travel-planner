package com.noblesi.travelplanner.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanRequest;

@Component
public class TravelPlanRequestValidator {

	private static final int MAX_TRAVEL_DAYS = 14;
	private final Clock clock;

	public TravelPlanRequestValidator(Clock clock) {
		this.clock = clock;
	}

	public void validate(CreateTravelPlanRequest request) {
		validateNewPlanDates(request.startDate(), request.endDate());
	}

	// 신규 생성과 공개 플랜 가져오기가 동일한 14일/과거 시작일 정책을 공유하도록 검증 진입점을 제공한다.
	public void validateNewPlanDates(LocalDate startDate, LocalDate endDate) {
		validateDates(startDate, endDate);
		requireTodayOrFutureStartDate(startDate);
	}

	public void validateDateChangePolicy(
			PlanEditorPlan existingPlan,
			LocalDate startDate,
			LocalDate endDate
	) {
		LocalDate today = LocalDate.now(clock);

		if (existingPlan.endDate().isBefore(today)) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"COMPLETED_TRAVEL_DATES_LOCKED",
					"종료된 여행 플랜의 날짜는 변경할 수 없습니다."
			);
		}

		boolean ongoing = !existingPlan.startDate().isAfter(today);
		if (ongoing) {
			if (!existingPlan.startDate().equals(startDate)) {
				throw new BusinessException(
						HttpStatus.BAD_REQUEST,
						"ONGOING_TRAVEL_START_DATE_LOCKED",
						"진행 중인 여행의 시작일은 변경할 수 없습니다."
				);
			}
			if (endDate.isBefore(today)) {
				throw new BusinessException(
						HttpStatus.BAD_REQUEST,
						"PAST_TRAVEL_END_DATE",
						"진행 중인 여행의 종료일은 오늘보다 빠를 수 없습니다."
				);
			}
			return;
		}

		requireTodayOrFutureStartDate(startDate);
	}

	public void validateDates(LocalDate startDate, LocalDate endDate) {
		if (startDate == null || endDate == null) {
			return;
		}

		if (startDate.isAfter(endDate)) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"INVALID_TRAVEL_DATE_RANGE",
					"여행 시작일은 종료일보다 늦을 수 없습니다."
			);
		}

		long travelDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		if (travelDays > MAX_TRAVEL_DAYS) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"TRAVEL_PLAN_DURATION_EXCEEDED",
					"여행 기간은 최대 14일까지 설정할 수 있습니다."
			);
		}
	}

	private void requireTodayOrFutureStartDate(LocalDate startDate) {
		if (startDate != null && startDate.isBefore(LocalDate.now(clock))) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"PAST_TRAVEL_START_DATE",
					"여행 시작일은 오늘보다 빠를 수 없습니다."
			);
		}
	}
}
