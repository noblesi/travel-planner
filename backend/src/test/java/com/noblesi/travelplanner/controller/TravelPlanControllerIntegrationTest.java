package com.noblesi.travelplanner.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TravelPlanControllerIntegrationTest {

	private static final String FORCE_FAILURE_CONSTRAINT = "CK_TEST_FORCE_PLAN_DAY_FAILURE";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanPlanData() {
		deletePlanData();
	}

	@AfterEach
	void cleanUp() {
		dropFailureConstraintIfPresent();
		deletePlanData();
	}

	@Test
	void createsOneDayTravelPlan() throws Exception {
		mockMvc.perform(post("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("1", "2026-08-10", "2026-08-10", "PRIVATE")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("/api/plans/\\d+/editor")))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.planId").isString())
				.andExpect(jsonPath("$.data.title").value("서울특별시 여행"))
				.andExpect(jsonPath("$.data.region.regionCode").value("1"))
				.andExpect(jsonPath("$.data.region.regionName").value("서울특별시"))
				.andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
				.andExpect(jsonPath("$.data.publishStatus").value("DRAFT"))
				.andExpect(jsonPath("$.data.versionNo").value(0))
				.andExpect(jsonPath("$.data.days", hasSize(1)))
				.andExpect(jsonPath("$.data.days[0].planDayId").isString())
				.andExpect(jsonPath("$.data.days[0].dayNo").value(1))
				.andExpect(jsonPath("$.data.days[0].travelDate").value("2026-08-10"))
				.andExpect(jsonPath("$.data.days[0].scheduleVersion").value(0));

		assertRowCount("TRAVEL_PLAN", 1);
		assertRowCount("PLAN_MEMBER", 1);
		assertRowCount("PLAN_DAY", 1);
	}

	@Test
	void createsFourteenDayTravelPlan() throws Exception {
		mockMvc.perform(post("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("6", "2026-08-04", "2026-08-17", "PUBLIC")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.days", hasSize(14)))
				.andExpect(jsonPath("$.data.days[13].dayNo").value(14))
				.andExpect(jsonPath("$.data.days[13].travelDate").value("2026-08-17"));

		assertRowCount("TRAVEL_PLAN", 1);
		assertRowCount("PLAN_MEMBER", 1);
		assertRowCount("PLAN_DAY", 14);
	}

	@Test
	void rejectsTravelPlanStartingBeforeTodayInKorea() throws Exception {
		mockMvc.perform(post("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("1", "2026-08-03", "2026-08-04", "PRIVATE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PAST_TRAVEL_START_DATE"));

		assertAllPlanTablesEmpty();
	}

	@Test
	void rejectsTravelPlanLongerThanFourteenDays() throws Exception {
		mockMvc.perform(post("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("1", "2026-08-01", "2026-08-15", "PRIVATE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("TRAVEL_PLAN_DURATION_EXCEEDED"));

		assertAllPlanTablesEmpty();
	}

	@Test
	void rejectsReversedTravelDates() throws Exception {
		mockMvc.perform(post("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("1", "2026-08-11", "2026-08-10", "PRIVATE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRAVEL_DATE_RANGE"));

		assertAllPlanTablesEmpty();
	}

	@Test
	void rejectsUnknownRegion() throws Exception {
		mockMvc.perform(post("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("unknown", "2026-08-10", "2026-08-12", "PRIVATE")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("REGION_NOT_FOUND"));

		assertAllPlanTablesEmpty();
	}

	@Test
	void rejectsMissingRequiredValue() throws Exception {
		mockMvc.perform(post("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "startDate": "2026-08-10",
						  "endDate": "2026-08-12",
						  "visibility": "PRIVATE"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("regionCode"));

		assertAllPlanTablesEmpty();
	}

	@Test
	void rejectsMalformedRequestValue() throws Exception {
		mockMvc.perform(post("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("1", "not-a-date", "2026-08-12", "PRIVATE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

		assertAllPlanTablesEmpty();
	}

	@Test
	void rollsBackAllRowsWhenPlanDayInsertFails() throws Exception {
		jdbcTemplate.execute("ALTER TABLE PLAN_DAY ADD CONSTRAINT " + FORCE_FAILURE_CONSTRAINT
				+ " CHECK (DAY_NO < 1)");

		try {
			mockMvc.perform(post("/api/plans")
					.contentType(MediaType.APPLICATION_JSON)
					.content(createRequest("1", "2026-08-10", "2026-08-12", "PRIVATE")))
					.andExpect(status().isInternalServerError())
					.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));

			assertAllPlanTablesEmpty();
		} finally {
			dropFailureConstraintIfPresent();
		}
	}

	private String createRequest(
			String regionCode,
			String startDate,
			String endDate,
			String visibility
	) {
		return """
				{
				  "regionCode": "%s",
				  "startDate": "%s",
				  "endDate": "%s",
				  "visibility": "%s"
				}
				""".formatted(regionCode, startDate, endDate, visibility);
	}

	private void assertAllPlanTablesEmpty() {
		assertRowCount("PLAN_SCHEDULE_ITEM", 0);
		assertRowCount("PLAN_DAY", 0);
		assertRowCount("PLAN_MEMBER", 0);
		assertRowCount("TRAVEL_PLAN", 0);
	}

	private void assertRowCount(String tableName, int expectedCount) {
		Integer actualCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + tableName,
				Integer.class
		);
		org.assertj.core.api.Assertions.assertThat(actualCount).isEqualTo(expectedCount);
	}

	private void deletePlanData() {
		jdbcTemplate.update("DELETE FROM PLAN_SCHEDULE_ITEM");
		jdbcTemplate.update("DELETE FROM PLAN_DAY");
		jdbcTemplate.update("DELETE FROM PLAN_MEMBER");
		jdbcTemplate.update("DELETE FROM TRAVEL_PLAN");
	}

	private void dropFailureConstraintIfPresent() {
		Integer constraintCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
						+ "WHERE CONSTRAINT_NAME = ?",
				Integer.class,
				FORCE_FAILURE_CONSTRAINT
		);
		if (constraintCount != null && constraintCount > 0) {
			jdbcTemplate.execute("ALTER TABLE PLAN_DAY DROP CONSTRAINT " + FORCE_FAILURE_CONSTRAINT);
		}
	}
}
