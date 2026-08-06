package com.noblesi.travelplanner.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.config.TravelTimeConfig;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.domain.plan.PlanVisibility;

class TravelPlanRequestValidatorTest {

	private TravelPlanRequestValidator validator;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(
				Instant.parse("2026-08-03T15:00:00Z"),
				TravelTimeConfig.TRAVEL_ZONE_ID
		);
		validator = new TravelPlanRequestValidator(clock);
	}

	@Test
	void allowsAnOngoingPlanToKeepItsStartAndChangeItsEnd() {
		PlanEditorPlan ongoingPlan = plan("2026-08-01", "2026-08-06");

		assertThatCode(() -> validator.validateDateChangePolicy(
				ongoingPlan,
				LocalDate.parse("2026-08-01"),
				LocalDate.parse("2026-08-07")
		)).doesNotThrowAnyException();
	}

	@Test
	void rejectsMovingAnUpcomingPlanIntoThePast() {
		PlanEditorPlan upcomingPlan = plan("2026-08-10", "2026-08-11");

		assertThatThrownBy(() -> validator.validateDateChangePolicy(
				upcomingPlan,
				LocalDate.parse("2026-08-03"),
				LocalDate.parse("2026-08-04")
		))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						org.assertj.core.api.Assertions.assertThat(exception.getCode())
								.isEqualTo("PAST_TRAVEL_START_DATE"));
	}

	private PlanEditorPlan plan(String startDate, String endDate) {
		return new PlanEditorPlan(
				1L,
				"서울 여행",
				"1",
				"서울특별시",
				LocalDate.parse(startDate),
				LocalDate.parse(endDate),
				PlanVisibility.PRIVATE,
				3
		);
	}
}
