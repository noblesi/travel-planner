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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	private static final long TARGET_DAY_ID = 9_007_199_254_741_004L;
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
		insertPlaceMaster("100", "경복궁", "ATTRACTION", "관광지", "https://example.com/place.jpg");
		insertPlaceMaster("101", "창덕궁", "ATTRACTION", "관광지", "https://example.com/changdeok.jpg");
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
				.andExpect(jsonPath("$.data.editor.plan.thumbnailImageUrl")
						.value("https://example.com/place.jpg"))
				.andExpect(jsonPath("$.data.editor.days[0].scheduleVersion").value(1))
				.andExpect(jsonPath("$.data.editor.days[0].items", hasSize(1)))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].placeName").value("경복궁"))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].positionNo").value(1));

		assertValue("SELECT COUNT(*) FROM PLAN_SCHEDULE_ITEM", 1);
		assertValue("SELECT SCHEDULE_VERSION FROM PLAN_DAY WHERE PLAN_DAY_ID = " + DAY_ID, 1);
		assertValue("SELECT LENGTH(REQUEST_HASH) FROM PLAN_EDIT_OPERATION", 64);
	}

	@Test
	void usesServerCatalogWhenClientSnapshotFieldsAreNull() throws Exception {
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
				.andExpect(jsonPath("$.data.editor.days[0].items[0].categoryName").value("관광지"))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].imageUrl")
						.value("https://example.com/place.jpg"));

		assertValue("""
				SELECT COUNT(*)
				  FROM PLAN_SCHEDULE_ITEM
				 WHERE CATEGORY_SNAPSHOT = '관광지'
				   AND ADDRESS_SNAPSHOT = '서울특별시 종로구'
				   AND IMAGE_URL_SNAPSHOT = 'https://example.com/place.jpg'
				   AND DESCRIPTION_SNAPSHOT IS NULL
				""", 1);
	}

	@Test
	void ignoresClientControlledThumbnailFields() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);

		mockMvc.perform(post(itemsPath())
				.contentType(MediaType.APPLICATION_JSON)
				.content(addRequest(ADD_OPERATION_ID, 0, "100", "조작된 장소명")
						.replace("\"관광지\"", "\"음식점\"")
						.replace("https://example.com/place.jpg", "https://attacker.example/fake.jpg")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.editor.days[0].items[0].placeName").value("경복궁"))
				.andExpect(jsonPath("$.data.editor.days[0].items[0].categoryName").value("관광지"))
				.andExpect(jsonPath("$.data.editor.plan.thumbnailImageUrl")
						.value("https://example.com/place.jpg"));
	}

	@Test
	void rejectsPlaceThatWasNotResolvedByServerSearch() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);

		mockMvc.perform(post(itemsPath())
				.contentType(MediaType.APPLICATION_JSON)
				.content(addRequest(ADD_OPERATION_ID, 0, "999", "알 수 없는 장소")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PLACE_REFERENCE_NOT_FOUND"));

		assertValue("SELECT COUNT(*) FROM PLAN_SCHEDULE_ITEM", 0);
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
		jdbcTemplate.update("DELETE FROM PLACE_MASTER WHERE EXTERNAL_PLACE_ID = '100'");

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
	void serializesConcurrentRetriesOfSameOperation() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);
		String request = addRequest(ADD_OPERATION_ID, 0, "100", "경복궁");
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			java.util.concurrent.Callable<String> mutation = () -> {
				ready.countDown();
				start.await();
				var response = mockMvc.perform(post(itemsPath())
							.contentType(MediaType.APPLICATION_JSON)
							.content(request))
						.andReturn()
						.getResponse();
				return response.getStatus() + ":" + response.getContentAsString();
			};
			Future<String> first = executor.submit(mutation);
			Future<String> second = executor.submit(mutation);
			ready.await();
			start.countDown();

			org.assertj.core.api.Assertions.assertThat(List.of(first.get(), second.get()))
					.allMatch(response -> response.startsWith("201:"));
		}

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
	void movesItemToAnotherDayAndUpdatesBothDayVersions() throws Exception {
		insertPlan(1L);
		insertPlanDay(1);
		insertPlanDay(TARGET_DAY_ID, 2, "2026-08-11", 4);
		insertScheduleItem(FIRST_ITEM_ID, "MORNING", 1, "100", "경복궁", 0);

		mockMvc.perform(patch(itemsPath() + "/" + FIRST_ITEM_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "operationId": "%s",
						  "scheduleVersion": 1,
						  "itemVersion": 0,
						  "timeSlot": "AFTERNOON",
						  "targetPlanDayId": "%s",
						  "targetScheduleVersion": 4
						}
						""".formatted(UPDATE_OPERATION_ID, TARGET_DAY_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.editor.days[0].scheduleVersion").value(2))
				.andExpect(jsonPath("$.data.editor.days[0].items", hasSize(0)))
				.andExpect(jsonPath("$.data.editor.days[1].scheduleVersion").value(5))
				.andExpect(jsonPath("$.data.editor.days[1].items[0].scheduleItemId")
						.value(Long.toString(FIRST_ITEM_ID)))
				.andExpect(jsonPath("$.data.editor.days[1].items[0].timeSlot").value("AFTERNOON"));
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
	void selectsNextEligibleThumbnailAfterDeletingCurrentThumbnailPlace() throws Exception {
		insertPlan(1L);
		insertPlanDay(0);
		insertScheduleItem(FIRST_ITEM_ID, "MORNING", 1, "100", "경복궁", 0);
		insertScheduleItem(SECOND_ITEM_ID, "MORNING", 2, "101", "창덕궁", 0);
		jdbcTemplate.update("UPDATE TRAVEL_PLAN SET THUMBNAIL_IMG = ? WHERE PLAN_ID = ?",
				"https://example.com/place.jpg", PLAN_ID);

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
				.andExpect(jsonPath("$.data.editor.plan.thumbnailImageUrl")
						.value("https://example.com/changdeok.jpg"));
	}

	@Test
	void clearsThumbnailWhenOnlyRestaurantRemains() throws Exception {
		insertPlaceMaster("200", "서울 맛집", "RESTAURANT", "음식점",
				"https://example.com/restaurant.jpg");
		insertPlan(1L);
		insertPlanDay(0);
		insertScheduleItem(FIRST_ITEM_ID, "MORNING", 1, "100", "경복궁", 0);
		insertScheduleItem(SECOND_ITEM_ID, "MORNING", 2, "200", "서울 맛집", 0);
		jdbcTemplate.update("UPDATE TRAVEL_PLAN SET THUMBNAIL_IMG = ? WHERE PLAN_ID = ?",
				"https://example.com/place.jpg", PLAN_ID);

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
				.andExpect(jsonPath("$.data.editor.plan.thumbnailImageUrl").value(org.hamcrest.Matchers.nullValue()));
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
	void allowsInvitedPlanMemberToEditSchedule() throws Exception {
		insertPlan(2L);
		insertPlanMember(1L, "INVITEE");
		insertPlanDay(0);

		mockMvc.perform(post(itemsPath())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addRequest(ADD_OPERATION_ID, 0, "100", "경복궁")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.editor.days[0].items[0].placeName").value("경복궁"));
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
				          DATE '2026-08-11', 'PRIVATE', 'ACTIVE', 0)
				""", PLAN_ID, ownerMemberId);
	}

	private void insertPlanDay(int scheduleVersion) {
		insertPlanDay(DAY_ID, 1, "2026-08-10", scheduleVersion);
	}

	private void insertPlanDay(long planDayId, int dayNo, String travelDate, int scheduleVersion) {
		jdbcTemplate.update("""
				INSERT INTO PLAN_DAY (
				    PLAN_DAY_ID, PLAN_ID, DAY_NO, TRAVEL_DATE, SCHEDULE_VERSION
				) VALUES (?, ?, ?, CAST(? AS DATE), ?)
				""", planDayId, PLAN_ID, dayNo, travelDate, scheduleVersion);
	}

	private void insertPlanMember(long memberId, String participantType) {
		jdbcTemplate.update("""
				INSERT INTO PLAN_MEMBER (PLAN_ID, MEMBER_ID, PARTICIPANT_TYPE)
				VALUES (?, ?, ?)
				""", PLAN_ID, memberId, participantType);
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

	private void insertPlaceMaster(
			String externalPlaceId,
			String placeName,
			String placeType,
			String categoryName,
			String imageUrl
	) {
		jdbcTemplate.update("""
				INSERT INTO PLACE_MASTER (
				    PLACE_PROVIDER, EXTERNAL_PLACE_ID, PLACE_TYPE, PLACE_NAME,
				    CATEGORY_NAME, ADDRESS, LATITUDE, LONGITUDE, IMAGE_URL, ACTIVE_YN
				) VALUES ('TOUR_API', ?, ?, ?, ?, '서울특별시 종로구',
				          37.579617, 126.977041, ?, 'Y')
				""", externalPlaceId, placeType, placeName, categoryName, imageUrl);
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
		jdbcTemplate.update("DELETE FROM PLACE_MASTER");
	}
}
