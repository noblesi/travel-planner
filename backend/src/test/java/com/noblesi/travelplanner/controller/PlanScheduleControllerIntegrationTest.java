package com.noblesi.travelplanner.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class PlanScheduleControllerIntegrationTest {

	private static final long PLAN_ID = 9_007_199_254_740_993L;
	private static final long DAY_ID = 9_007_199_254_740_994L;
	private static final long FIRST_ITEM_ID = 9_007_199_254_740_995L;
	private static final long SECOND_ITEM_ID = 9_007_199_254_740_996L;
	private static final long THIRD_ITEM_ID = 9_007_199_254_740_997L;

	private static final String ADD_OPERATION_ID = "11111111-1111-4111-8111-111111111111";
	private static final String UPDATE_OPERATION_ID = "22222222-2222-4222-8222-222222222222";
	private static final String DELETE_OPERATION_ID = "33333333-3333-4333-8333-333333333333";
	private static final String REORDER_OPERATION_ID = "44444444-4444-4444-8444-444444444444";

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
	void addsScheduleItemAtEndAndReturnsUpdatedEditor() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);

		mockMvc.perform(post(itemsPath())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addRequest(ADD_OPERATION_ID, 0, "100", "경복궁")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/items/\\d+")))
				.andExpect(jsonPath("$.data.operationId").value(ADD_OPERATION_ID))
				.andExpect(jsonPath("$.data.scheduleItemId").isString())
				.andExpect(jsonPath("$.data.resultScheduleVersion").value(1))
				.andExpect(jsonPath("$.data.editor.days[0].scheduleVersion").value(1))
				.andExpect(jsonPath("$.data.editor.days[0].items", hasSize(1)))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].placeName").value("경복궁"))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].positionNo").value(1));

		assertValue("SELECT COUNT(*) FROM PLAN_SCHEDULE_ITEM", 1);
		assertValue("SELECT SCHEDULE_VERSION FROM PLAN_DAY WHERE PLAN_DAY_ID = " + DAY_ID, 1);
		assertValue("SELECT LENGTH(REQUEST_HASH) FROM PLAN_EDIT_OPERATION", 64);
	}

	@Test
	void addsScheduleItemWhenOptionalSnapshotFieldsAreNull() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);

		mockMvc.perform(post(itemsPath())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "operationId": "%s",
							  "scheduleVersion": 0,
							  "timeSlot": "MORNING",
							  "placeProvider": "TOUR_API",
							  "externalPlaceId": "100",
							  "placeName": "경복궁",
							  "categoryName": null,
							  "address": null,
							  "latitude": null,
							  "longitude": null,
							  "imageUrl": null,
							  "description": null
							}
							""".formatted(ADD_OPERATION_ID)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.editor.days[0].items[0].categoryName").isEmpty())
				.andExpect(jsonPath("$.data.editor.days[0].items[0].imageUrl").isEmpty());

		assertValue("""
				SELECT COUNT(*)
				  FROM PLAN_SCHEDULE_ITEM
				 WHERE CATEGORY_SNAPSHOT IS NULL
				   AND ADDRESS_SNAPSHOT IS NULL
				   AND LATITUDE_SNAPSHOT IS NULL
				   AND LONGITUDE_SNAPSHOT IS NULL
				   AND IMAGE_URL_SNAPSHOT IS NULL
				   AND DESCRIPTION_SNAPSHOT IS NULL
				""", 1);
	}

	@Test
	void replaysSameOperationWithoutApplyingItTwice() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);
		String request = addRequest(ADD_OPERATION_ID, 0, "100", "경복궁");

		mockMvc.perform(post(itemsPath())
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isCreated());

		mockMvc.perform(post(itemsPath())
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.scheduleItemId").isString())
				.andExpect(jsonPath("$.data.resultScheduleVersion").value(1));

		assertValue("SELECT COUNT(*) FROM PLAN_SCHEDULE_ITEM", 1);
		assertValue("SELECT SCHEDULE_VERSION FROM PLAN_DAY WHERE PLAN_DAY_ID = " + DAY_ID, 1);
		assertValue("SELECT COUNT(*) FROM PLAN_EDIT_OPERATION", 1);
	}

	@Test
	void rejectsOperationIdReusedWithDifferentPayload() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);

		mockMvc.perform(post(itemsPath())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addRequest(ADD_OPERATION_ID, 0, "100", "경복궁")))
				.andExpect(status().isCreated());

		mockMvc.perform(post(itemsPath())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addRequest(ADD_OPERATION_ID, 0, "101", "창덕궁")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_OPERATION"));

		assertValue("SELECT COUNT(*) FROM PLAN_SCHEDULE_ITEM", 1);
	}

	@Test
	void rejectsStaleScheduleVersionWithoutPartialData() throws Exception {
		insertPlan(1L);
		insertPlanDay(2);

		mockMvc.perform(post(itemsPath())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addRequest(ADD_OPERATION_ID, 1, "100", "경복궁")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SCHEDULE_VERSION_CONFLICT"));

		assertValue("SELECT COUNT(*) FROM PLAN_SCHEDULE_ITEM", 0);
		assertValue("SELECT COUNT(*) FROM PLAN_EDIT_OPERATION", 0);
		assertValue("SELECT SCHEDULE_VERSION FROM PLAN_DAY WHERE PLAN_DAY_ID = " + DAY_ID, 2);
	}

	@Test
	void rollsBackDayVersionWhenItemInsertFails() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);
		insertScheduleItem(FIRST_ITEM_ID, "AFTERNOON", 1, "200", "남산서울타워", 0);
		jdbcTemplate.execute("ALTER SEQUENCE SEQ_PLAN_SCHEDULE_ITEM RESTART WITH " + FIRST_ITEM_ID);

		mockMvc.perform(post(itemsPath())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addRequest(ADD_OPERATION_ID, 0, "100", "경복궁")))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));

		assertValue("SELECT COUNT(*) FROM PLAN_SCHEDULE_ITEM", 1);
		assertValue("SELECT SCHEDULE_VERSION FROM PLAN_DAY WHERE PLAN_DAY_ID = " + DAY_ID, 0);
		assertValue("SELECT COUNT(*) FROM PLAN_EDIT_OPERATION", 0);
	}

	@Test
	void movesItemToAnotherTimeSlotAndCompactsSourcePositions() throws Exception {
		insertPlan(1L);
		insertPlanDay(2);
		insertScheduleItem(FIRST_ITEM_ID, "MORNING", 1, "100", "경복궁", 0);
		insertScheduleItem(SECOND_ITEM_ID, "MORNING", 2, "101", "창덕궁", 0);

		mockMvc.perform(patch(itemsPath() + "/" + FIRST_ITEM_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "operationId": "%s",
							  "scheduleVersion": 2,
							  "itemVersion": 0,
							  "timeSlot": "AFTERNOON"
							}
							""".formatted(UPDATE_OPERATION_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.resultScheduleVersion").value(3))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].scheduleItemId")
						.value(Long.toString(SECOND_ITEM_ID)))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].positionNo").value(1))
				.andExpect(jsonPath("$.data.editor.days[0].items[1].scheduleItemId")
						.value(Long.toString(FIRST_ITEM_ID)))
				.andExpect(jsonPath("$.data.editor.days[0].items[1].timeSlot").value("AFTERNOON"))
				.andExpect(jsonPath("$.data.editor.days[0].items[1].itemVersion").value(1));
	}

	@Test
	void rejectsStaleItemVersionAndRollsBackDayVersion() throws Exception {
		insertPlan(1L);
		insertPlanDay(2);
		insertScheduleItem(FIRST_ITEM_ID, "MORNING", 1, "100", "경복궁", 1);

		mockMvc.perform(patch(itemsPath() + "/" + FIRST_ITEM_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "operationId": "%s",
							  "scheduleVersion": 2,
							  "itemVersion": 0,
							  "timeSlot": "AFTERNOON"
							}
							""".formatted(UPDATE_OPERATION_ID)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ITEM_VERSION_CONFLICT"));

		assertValue("SELECT SCHEDULE_VERSION FROM PLAN_DAY WHERE PLAN_DAY_ID = " + DAY_ID, 2);
		assertValue("SELECT COUNT(*) FROM PLAN_EDIT_OPERATION", 0);
	}

	@Test
	void deletesItemAndCompactsRemainingPositions() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);
		insertScheduleItem(FIRST_ITEM_ID, "MORNING", 1, "100", "경복궁", 0);
		insertScheduleItem(SECOND_ITEM_ID, "MORNING", 2, "101", "창덕궁", 0);

		mockMvc.perform(delete(itemsPath() + "/" + FIRST_ITEM_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "operationId": "%s",
							  "scheduleVersion": 0,
							  "itemVersion": 0
							}
							""".formatted(DELETE_OPERATION_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.scheduleItemId").value(Long.toString(FIRST_ITEM_ID)))
				.andExpect(jsonPath("$.data.resultScheduleVersion").value(1))
				.andExpect(jsonPath("$.data.editor.days[0].items", hasSize(1)))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].scheduleItemId")
						.value(Long.toString(SECOND_ITEM_ID)))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].positionNo").value(1));
	}

	@Test
	void reordersAllItemsInSelectedTimeSlot() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);
		insertScheduleItem(FIRST_ITEM_ID, "MORNING", 1, "100", "경복궁", 0);
		insertScheduleItem(SECOND_ITEM_ID, "MORNING", 2, "101", "창덕궁", 0);
		insertScheduleItem(THIRD_ITEM_ID, "MORNING", 3, "102", "덕수궁", 0);

		mockMvc.perform(put(itemsPath() + "/order")
					.contentType(MediaType.APPLICATION_JSON)
					.content(reorderRequest(
							THIRD_ITEM_ID,
							FIRST_ITEM_ID,
							SECOND_ITEM_ID
					)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.resultScheduleVersion").value(1))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].scheduleItemId")
						.value(Long.toString(THIRD_ITEM_ID)))
				.andExpect(jsonPath("$.data.editor.days[0].items[1].scheduleItemId")
						.value(Long.toString(FIRST_ITEM_ID)))
				.andExpect(jsonPath("$.data.editor.days[0].items[2].scheduleItemId")
						.value(Long.toString(SECOND_ITEM_ID)));
	}

	@Test
	void rejectsIncompleteReorderWithoutChangingPositions() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);
		insertScheduleItem(FIRST_ITEM_ID, "MORNING", 1, "100", "경복궁", 0);
		insertScheduleItem(SECOND_ITEM_ID, "MORNING", 2, "101", "창덕궁", 0);

		mockMvc.perform(put(itemsPath() + "/order")
					.contentType(MediaType.APPLICATION_JSON)
					.content(reorderRequest(SECOND_ITEM_ID)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_SCHEDULE_ORDER"));

		assertValue("SELECT POSITION_NO FROM PLAN_SCHEDULE_ITEM WHERE SCHEDULE_ITEM_ID = "
				+ FIRST_ITEM_ID, 1);
		assertValue("SELECT POSITION_NO FROM PLAN_SCHEDULE_ITEM WHERE SCHEDULE_ITEM_ID = "
				+ SECOND_ITEM_ID, 2);
		assertValue("SELECT SCHEDULE_VERSION FROM PLAN_DAY WHERE PLAN_DAY_ID = " + DAY_ID, 0);
		assertValue("SELECT COUNT(*) FROM PLAN_EDIT_OPERATION", 0);
	}

	@Test
	void hidesAnotherMembersPlan() throws Exception {
		insertPlan(2L);
		insertPlanDay(0);

		mockMvc.perform(post(itemsPath())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addRequest(ADD_OPERATION_ID, 0, "100", "경복궁")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
	}

	private String itemsPath() {
		return "/api/plans/" + PLAN_ID + "/days/" + DAY_ID + "/items";
	}

	private String addRequest(
			String operationId,
			int scheduleVersion,
			String externalPlaceId,
			String placeName
	) {
		return """
				{
				  "operationId": "%s",
				  "scheduleVersion": %d,
				  "timeSlot": "MORNING",
				  "placeProvider": "TOUR_API",
				  "externalPlaceId": "%s",
				  "placeName": "%s",
				  "categoryName": "관광지",
				  "address": "서울특별시 종로구",
				  "latitude": 37.579617,
				  "longitude": 126.977041,
				  "imageUrl": "https://example.com/place.jpg",
				  "description": null
				}
				""".formatted(operationId, scheduleVersion, externalPlaceId, placeName);
	}

	private String reorderRequest(long... itemIds) {
		StringBuilder ids = new StringBuilder();
		for (int index = 0; index < itemIds.length; index++) {
			if (index > 0) {
				ids.append(", ");
			}
			ids.append('"').append(itemIds[index]).append('"');
		}
		return """
				{
				  "operationId": "%s",
				  "scheduleVersion": 0,
				  "timeSlot": "MORNING",
				  "scheduleItemIds": [%s]
				}
				""".formatted(REORDER_OPERATION_ID, ids);
	}

	private void insertPlan(long ownerMemberId) {
		jdbcTemplate.update("""
				INSERT INTO TRAVEL_PLAN (
				    PLAN_ID, OWNER_MEMBER_ID, TITLE, REGION_CODE,
				    START_DATE, END_DATE, VISIBILITY, PLAN_STATUS, VERSION_NO
				) VALUES (?, ?, '서울특별시 여행', '1', DATE '2026-08-10',
				          DATE '2026-08-10', 'PRIVATE', 'ACTIVE', 0)
				""", PLAN_ID, ownerMemberId);
	}

	private void insertPlanDay(int scheduleVersion) {
		jdbcTemplate.update("""
				INSERT INTO PLAN_DAY (
				    PLAN_DAY_ID, PLAN_ID, DAY_NO, TRAVEL_DATE, SCHEDULE_VERSION
				) VALUES (?, ?, 1, DATE '2026-08-10', ?)
				""", DAY_ID, PLAN_ID, scheduleVersion);
	}

	private void insertScheduleItem(
			long itemId,
			String timeSlot,
			int positionNo,
			String externalPlaceId,
			String placeName,
			int itemVersion
	) {
		jdbcTemplate.update("""
				INSERT INTO PLAN_SCHEDULE_ITEM (
				    SCHEDULE_ITEM_ID, PLAN_DAY_ID, TIME_SLOT, POSITION_NO,
				    PLACE_PROVIDER, EXTERNAL_PLACE_ID, PLACE_NAME_SNAPSHOT,
				    CATEGORY_SNAPSHOT, ADDRESS_SNAPSHOT, LATITUDE_SNAPSHOT,
				    LONGITUDE_SNAPSHOT, IMAGE_URL_SNAPSHOT, ITEM_VERSION
				) VALUES (?, ?, ?, ?, 'TOUR_API', ?, ?, '관광지',
				          '서울특별시 종로구', 37.579617, 126.977041,
				          'https://example.com/place.jpg', ?)
				""", itemId, DAY_ID, timeSlot, positionNo, externalPlaceId, placeName, itemVersion);
	}

	private void assertValue(String sql, int expected) {
		Integer actual = jdbcTemplate.queryForObject(sql, Integer.class);
		org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
	}

	private void deletePlanData() {
		jdbcTemplate.update("DELETE FROM PLAN_EDIT_OPERATION");
		jdbcTemplate.update("DELETE FROM PLAN_SCHEDULE_ITEM");
		jdbcTemplate.update("DELETE FROM PLAN_DAY");
		jdbcTemplate.update("DELETE FROM PLAN_MEMBER");
		jdbcTemplate.update("DELETE FROM TRAVEL_PLAN");
	}
}
