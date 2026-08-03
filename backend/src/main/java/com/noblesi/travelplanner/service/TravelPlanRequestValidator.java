package com.noblesi.travelplanner.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanRequest;

@Component
public class TravelPlanRequestValidator {

	private static final int MAX_TRAVEL_DAYS = 14;

	public void validate(CreateTravelPlanRequest request) {
		validateDates(request.startDate(), request.endDate());
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
}
