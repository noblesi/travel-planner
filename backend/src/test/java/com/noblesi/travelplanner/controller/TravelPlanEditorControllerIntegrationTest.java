package com.noblesi.travelplanner.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class TravelPlanEditorControllerIntegrationTest {

	private static final long PLAN_ID = 9_007_199_254_740_993L;
	private static final long FIRST_DAY_ID = 9_007_199_254_740_994L;
	private static final long SECOND_DAY_ID = 9_007_199_254_740_995L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deletePlanData();
	}

	@AfterEach
	void cleanUp() {
		deletePlanData();
	}

	@Test
	void returnsOwnedActivePlanWithSortedDaysAndItems() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");
		insertPlanDay(FIRST_DAY_ID, PLAN_ID, 1, "2026-08-10", 2);
		insertPlanDay(SECOND_DAY_ID, PLAN_ID, 2, "2026-08-11", 0);
		insertScheduleItem(
				9_007_199_254_740_996L,
				FIRST_DAY_ID,
				"AFTERNOON",
				1,
				"200",
				"남산서울타워"
		);
		insertScheduleItem(
				9_007_199_254_740_997L,
				FIRST_DAY_ID,
				"MORNING",
				2,
				"101",
				"국립민속박물관"
		);
		insertScheduleItem(
				9_007_199_254_740_998L,
				FIRST_DAY_ID,
				"MORNING",
				1,
				"100",
				"경복궁"
		);

		mockMvc.perform(get("/api/plans/{planId}/editor", Long.toString(PLAN_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.plan.planId").value(Long.toString(PLAN_ID)))
				.andExpect(jsonPath("$.data.plan.title").value("서울특별시 여행"))
				.andExpect(jsonPath("$.data.plan.regionCode").value("1"))
				.andExpect(jsonPath("$.data.plan.regionName").value("서울특별시"))
				.andExpect(jsonPath("$.data.plan.startDate").value("2026-08-10"))
				.andExpect(jsonPath("$.data.plan.endDate").value("2026-08-11"))
				.andExpect(jsonPath("$.data.plan.visibility").value("PRIVATE"))
				.andExpect(jsonPath("$.data.plan.versionNo").value(3))
				.andExpect(jsonPath("$.data.days", hasSize(2)))
				.andExpect(jsonPath("$.data.days[0].planDayId").value(Long.toString(FIRST_DAY_ID)))
				.andExpect(jsonPath("$.data.days[0].dayNo").value(1))
				.andExpect(jsonPath("$.data.days[0].scheduleVersion").value(2))
				.andExpect(jsonPath("$.data.days[0].items", hasSize(3)))
				.andExpect(jsonPath("$.data.days[0].items[0].scheduleItemId")
						.value("9007199254740998"))
				.andExpect(jsonPath("$.data.days[0].items[0].timeSlot").value("MORNING"))
				.andExpect(jsonPath("$.data.days[0].items[0].positionNo").value(1))
				.andExpect(jsonPath("$.data.days[0].items[0].placeName").value("경복궁"))
				.andExpect(jsonPath("$.data.days[0].items[0].latitude").value(37.579617))
				.andExpect(jsonPath("$.data.days[0].items[0].description").value(nullValue()))
				.andExpect(jsonPath("$.data.days[0].items[1].placeName").value("국립민속박물관"))
				.andExpect(jsonPath("$.data.days[0].items[2].timeSlot").value("AFTERNOON"))
				.andExpect(jsonPath("$.data.days[1].dayNo").value(2))
				.andExpect(jsonPath("$.data.days[1].items", hasSize(0)));
	}

	@Test
	void returnsPlanNotFoundWhenPlanDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/plans/1/editor"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
	}

	@Test
	void updatesPlanMetadataAndKeepsExistingDays() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");
		insertPlanDay(FIRST_DAY_ID, PLAN_ID, 1, "2026-08-10", 0);
		insertPlanDay(SECOND_DAY_ID, PLAN_ID, 2, "2026-08-11", 0);

		mockMvc.perform(patch("/api/plans/{planId}", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title": "  서울 맛집 여행  ",
							  "visibility": "PUBLIC",
							  "versionNo": 3
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.plan.title").value("서울 맛집 여행"))
				.andExpect(jsonPath("$.data.plan.visibility").value("PUBLIC"))
				.andExpect(jsonPath("$.data.plan.versionNo").value(4))
				.andExpect(jsonPath("$.data.days", hasSize(2)))
				.andExpect(jsonPath("$.data.days[0].planDayId").value(Long.toString(FIRST_DAY_ID)));
	}

	@Test
	void keepsVersionWhenPlanMetadataDoesNotChange() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");

		mockMvc.perform(patch("/api/plans/{planId}", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title": "서울특별시 여행",
							  "visibility": "PRIVATE",
							  "versionNo": 3
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.plan.versionNo").value(3));
	}

	@Test
	void rejectsMetadataUpdateWhenPlanVersionIsStale() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");

		mockMvc.perform(patch("/api/plans/{planId}", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title": "서울 맛집 여행",
							  "visibility": "PUBLIC",
							  "versionNo": 2
							}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PLAN_VERSION_CONFLICT"));
	}

	@Test
	void rejectsInvalidPlanMetadata() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");

		mockMvc.perform(patch("/api/plans/{planId}", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title": "   ",
							  "visibility": null,
							  "versionNo": -1
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors", hasSize(3)));
	}

	@Test
	void rejectsPlanTitleLongerThanDatabaseLimit() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");
		String requestBody = """
				{
				  "title": "%s",
				  "visibility": "PRIVATE",
				  "versionNo": 3
				}
				""".formatted("가".repeat(201));

		mockMvc.perform(patch("/api/plans/{planId}", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void hidesAnotherMembersPlanDuringMetadataUpdate() throws Exception {
		insertPlan(PLAN_ID, 2L, "ACTIVE");

		mockMvc.perform(patch("/api/plans/{planId}", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title": "서울 맛집 여행",
							  "visibility": "PUBLIC",
							  "versionNo": 3
							}
							"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
	}

	@Test
	void expandsDateRangeAndKeepsSchedulesOnTheirTravelDates() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");
		insertPlanDay(FIRST_DAY_ID, PLAN_ID, 1, "2026-08-10", 2);
		insertPlanDay(SECOND_DAY_ID, PLAN_ID, 2, "2026-08-11", 0);
		insertScheduleItem(
				9_007_199_254_740_996L,
				FIRST_DAY_ID,
				"MORNING",
				1,
				"100",
				"경복궁"
		);

		mockMvc.perform(patch("/api/plans/{planId}/dates", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "startDate": "2026-08-09",
							  "endDate": "2026-08-12",
							  "versionNo": 3,
							  "force": false
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.plan.startDate").value("2026-08-09"))
				.andExpect(jsonPath("$.data.plan.endDate").value("2026-08-12"))
				.andExpect(jsonPath("$.data.plan.versionNo").value(4))
				.andExpect(jsonPath("$.data.days", hasSize(4)))
				.andExpect(jsonPath("$.data.days[1].planDayId").value(Long.toString(FIRST_DAY_ID)))
				.andExpect(jsonPath("$.data.days[1].dayNo").value(2))
				.andExpect(jsonPath("$.data.days[1].travelDate").value("2026-08-10"))
				.andExpect(jsonPath("$.data.days[1].items[0].placeName").value("경복궁"));
	}

	@Test
	void movesSameDurationRangeAndKeepsSchedulesOnTheSameDayNumber() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");
		insertPlanDay(FIRST_DAY_ID, PLAN_ID, 1, "2026-08-10", 2);
		insertPlanDay(SECOND_DAY_ID, PLAN_ID, 2, "2026-08-11", 0);
		insertScheduleItem(
				9_007_199_254_740_996L,
				FIRST_DAY_ID,
				"MORNING",
				1,
				"100",
				"경복궁"
		);

		mockMvc.perform(patch("/api/plans/{planId}/dates", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "startDate": "2026-08-11",
							  "endDate": "2026-08-12",
							  "versionNo": 3,
							  "force": false
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.days[0].planDayId").value(Long.toString(FIRST_DAY_ID)))
				.andExpect(jsonPath("$.data.days[0].travelDate").value("2026-08-11"))
				.andExpect(jsonPath("$.data.days[0].items[0].placeName").value("경복궁"))
				.andExpect(jsonPath("$.data.days[1].travelDate").value("2026-08-12"));
	}

	@Test
	void requiresConfirmationBeforeRemovingADayWithSchedules() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");
		insertPlanDay(FIRST_DAY_ID, PLAN_ID, 1, "2026-08-10", 2);
		insertPlanDay(SECOND_DAY_ID, PLAN_ID, 2, "2026-08-11", 0);
		insertScheduleItem(
				9_007_199_254_740_996L,
				FIRST_DAY_ID,
				"MORNING",
				1,
				"100",
				"경복궁"
		);

		mockMvc.perform(patch("/api/plans/{planId}/dates", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "startDate": "2026-08-11",
							  "endDate": "2026-08-11",
							  "versionNo": 3,
							  "force": false
							}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED"));

		mockMvc.perform(get("/api/plans/{planId}/editor", Long.toString(PLAN_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.plan.versionNo").value(3))
				.andExpect(jsonPath("$.data.days", hasSize(2)))
				.andExpect(jsonPath("$.data.days[0].items", hasSize(1)));
	}

	@Test
	void removesScheduledDayAfterConfirmation() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");
		insertPlanDay(FIRST_DAY_ID, PLAN_ID, 1, "2026-08-10", 2);
		insertPlanDay(SECOND_DAY_ID, PLAN_ID, 2, "2026-08-11", 0);
		insertScheduleItem(
				9_007_199_254_740_996L,
				FIRST_DAY_ID,
				"MORNING",
				1,
				"100",
				"경복궁"
		);

		mockMvc.perform(patch("/api/plans/{planId}/dates", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "startDate": "2026-08-11",
							  "endDate": "2026-08-11",
							  "versionNo": 3,
							  "force": true
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.plan.versionNo").value(4))
				.andExpect(jsonPath("$.data.days", hasSize(1)))
				.andExpect(jsonPath("$.data.days[0].planDayId").value(Long.toString(SECOND_DAY_ID)))
				.andExpect(jsonPath("$.data.days[0].dayNo").value(1))
				.andExpect(jsonPath("$.data.days[0].items", hasSize(0)));
	}

	@Test
	void rejectsDateUpdateWhenPlanVersionIsStale() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");
		insertPlanDay(FIRST_DAY_ID, PLAN_ID, 1, "2026-08-10", 0);
		insertPlanDay(SECOND_DAY_ID, PLAN_ID, 2, "2026-08-11", 0);

		mockMvc.perform(patch("/api/plans/{planId}/dates", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "startDate": "2026-08-10",
							  "endDate": "2026-08-12",
							  "versionNo": 2,
							  "force": false
							}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PLAN_VERSION_CONFLICT"));
	}

	@Test
	void rejectsDateUpdateLongerThanFourteenDays() throws Exception {
		insertPlan(PLAN_ID, 1L, "ACTIVE");
		insertPlanDay(FIRST_DAY_ID, PLAN_ID, 1, "2026-08-10", 0);
		insertPlanDay(SECOND_DAY_ID, PLAN_ID, 2, "2026-08-11", 0);

		mockMvc.perform(patch("/api/plans/{planId}/dates", Long.toString(PLAN_ID))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "startDate": "2026-08-01",
							  "endDate": "2026-08-15",
							  "versionNo": 3,
							  "force": false
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TRAVEL_PLAN_DURATION_EXCEEDED"));
	}

	@Test
	void returnsPlanNotFoundForAnotherMembersPlan() throws Exception {
		insertPlan(PLAN_ID, 2L, "ACTIVE");

		mockMvc.perform(get("/api/plans/{planId}/editor", Long.toString(PLAN_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
	}

	@Test
	void returnsPlanNotFoundForDeletedPlan() throws Exception {
		insertPlan(PLAN_ID, 1L, "DELETED");

		mockMvc.perform(get("/api/plans/{planId}/editor", Long.toString(PLAN_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-1", "abc", "9223372036854775808" })
	void rejectsInvalidPlanId(String planId) throws Exception {
		mockMvc.perform(get("/api/plans/{planId}/editor", planId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PATH_PARAMETER"));
	}

	private void insertPlan(long planId, long ownerMemberId, String status) {
		if ("DELETED".equals(status)) {
			jdbcTemplate.update("""
					INSERT INTO TRAVEL_PLAN (
					    PLAN_ID, OWNER_MEMBER_ID, TITLE, REGION_CODE,
					    START_DATE, END_DATE, VISIBILITY, PLAN_STATUS, VERSION_NO,
					    DELETED_AT, DELETED_BY_MEMBER_ID
					) VALUES (?, ?, '서울특별시 여행', '1', DATE '2026-08-10',
					          DATE '2026-08-11', 'PRIVATE', 'DELETED', 3,
					          CURRENT_TIMESTAMP, ?)
					""", planId, ownerMemberId, ownerMemberId);
			return;
		}

		jdbcTemplate.update("""
				INSERT INTO TRAVEL_PLAN (
				    PLAN_ID, OWNER_MEMBER_ID, TITLE, REGION_CODE,
				    START_DATE, END_DATE, VISIBILITY, PLAN_STATUS, VERSION_NO
				) VALUES (?, ?, '서울특별시 여행', '1', DATE '2026-08-10',
				          DATE '2026-08-11', 'PRIVATE', 'ACTIVE', 3)
				""", planId, ownerMemberId);
	}

	private void insertPlanDay(
			long planDayId,
			long planId,
			int dayNo,
			String travelDate,
			int scheduleVersion
	) {
		jdbcTemplate.update("""
				INSERT INTO PLAN_DAY (
				    PLAN_DAY_ID, PLAN_ID, DAY_NO, TRAVEL_DATE, SCHEDULE_VERSION
				) VALUES (?, ?, ?, CAST(? AS DATE), ?)
				""", planDayId, planId, dayNo, travelDate, scheduleVersion);
	}

	private void insertScheduleItem(
			long itemId,
			long planDayId,
			String timeSlot,
			int positionNo,
			String externalPlaceId,
			String placeName
	) {
		jdbcTemplate.update("""
				INSERT INTO PLAN_SCHEDULE_ITEM (
				    SCHEDULE_ITEM_ID, PLAN_DAY_ID, TIME_SLOT, POSITION_NO,
				    PLACE_PROVIDER, EXTERNAL_PLACE_ID, PLACE_NAME_SNAPSHOT,
				    CATEGORY_SNAPSHOT, ADDRESS_SNAPSHOT, LATITUDE_SNAPSHOT,
				    LONGITUDE_SNAPSHOT, IMAGE_URL_SNAPSHOT, ITEM_VERSION
				) VALUES (?, ?, ?, ?, 'TOUR_API', ?, ?, '관광지',
				          '서울특별시 종로구', 37.579617, 126.977041,
				          'https://example.com/place.jpg', 0)
				""", itemId, planDayId, timeSlot, positionNo, externalPlaceId, placeName);
	}

	private void deletePlanData() {
		jdbcTemplate.update("DELETE FROM PLAN_SCHEDULE_ITEM");
		jdbcTemplate.update("DELETE FROM PLAN_DAY");
		jdbcTemplate.update("DELETE FROM PLAN_MEMBER");
		jdbcTemplate.update("DELETE FROM TRAVEL_PLAN");
	}
}
