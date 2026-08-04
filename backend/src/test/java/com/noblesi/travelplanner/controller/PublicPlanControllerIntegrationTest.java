package com.noblesi.travelplanner.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_public;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PublicPlanControllerIntegrationTest {
	private static final long PUBLIC_PLAN_ID = 71_001L;
	private static final long PRIVATE_PLAN_ID = 71_002L;
	private static final long FIRST_DAY_ID = 72_001L;
	private static final long SECOND_DAY_ID = 72_002L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteFixtures();
		List<Long> memberIds = jdbcTemplate.queryForList(
				"SELECT MEMBER_ID FROM MEMBER ORDER BY MEMBER_ID",
				Long.class
		);
		long ownerMemberId = memberIds.get(0);

		jdbcTemplate.update("""
				INSERT INTO TRAVEL_PLAN (
				    PLAN_ID, OWNER_MEMBER_ID, TITLE, REGION_CODE, START_DATE, END_DATE,
				    VISIBILITY, PLAN_STATUS, VERSION_NO, THUMBNAIL_IMG, VIEW_COUNT
				) VALUES (?, ?, '서울 공개 산책', '1', DATE '2026-08-10', DATE '2026-08-11',
				          'PUBLIC', 'ACTIVE', 0, 'https://example.com/seoul.jpg', 41)
				""", PUBLIC_PLAN_ID, ownerMemberId);
		jdbcTemplate.update("""
				INSERT INTO TRAVEL_PLAN (
				    PLAN_ID, OWNER_MEMBER_ID, TITLE, REGION_CODE, START_DATE, END_DATE,
				    VISIBILITY, PLAN_STATUS, VERSION_NO, VIEW_COUNT
				) VALUES (?, ?, '서울 비공개 여행', '1', DATE '2026-08-10', DATE '2026-08-10',
				          'PRIVATE', 'ACTIVE', 0, 9)
				""", PRIVATE_PLAN_ID, ownerMemberId);

		jdbcTemplate.update(
				"INSERT INTO PLAN_DAY (PLAN_DAY_ID, PLAN_ID, DAY_NO, TRAVEL_DATE, SCHEDULE_VERSION) "
						+ "VALUES (?, ?, 1, DATE '2026-08-10', 0)",
				FIRST_DAY_ID,
				PUBLIC_PLAN_ID
		);
		jdbcTemplate.update(
				"INSERT INTO PLAN_DAY (PLAN_DAY_ID, PLAN_ID, DAY_NO, TRAVEL_DATE, SCHEDULE_VERSION) "
						+ "VALUES (?, ?, 2, DATE '2026-08-11', 0)",
				SECOND_DAY_ID,
				PUBLIC_PLAN_ID
		);
		jdbcTemplate.update("""
				INSERT INTO PLAN_SCHEDULE_ITEM (
				    SCHEDULE_ITEM_ID, PLAN_DAY_ID, TIME_SLOT, POSITION_NO,
				    PLACE_PROVIDER, EXTERNAL_PLACE_ID, PLACE_NAME_SNAPSHOT,
				    CATEGORY_SNAPSHOT, ADDRESS_SNAPSHOT, LATITUDE_SNAPSHOT,
				    LONGITUDE_SNAPSHOT, DESCRIPTION_SNAPSHOT, ITEM_VERSION
				) VALUES (73_001, ?, 'MORNING', 1, 'DEMO', 'SEOUL-1', '경복궁',
				          '역사관광', '서울 종로구', 37.579617, 126.977041,
				          '궁궐 산책', 0)
				""", FIRST_DAY_ID);

		for (int index = 0; index < Math.min(2, memberIds.size()); index++) {
			jdbcTemplate.update(
					"INSERT INTO PLAN_LIKE (PLAN_LIKE_ID, PLAN_ID, MEMBER_ID) VALUES (?, ?, ?)",
					74_001L + index,
					PUBLIC_PLAN_ID,
					memberIds.get(index)
			);
		}
	}

	@AfterEach
	void cleanUp() {
		deleteFixtures();
	}

	@Test
	void searchesOnlyActivePublicPlansWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/plans").queryParam("limit", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(1))
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.totalPages").value(1))
				.andExpect(jsonPath("$.data.hasNext").value(false))
				.andExpect(jsonPath("$.data.plans[0].planId").value(Long.toString(PUBLIC_PLAN_ID)))
				.andExpect(jsonPath("$.data.plans[0].regionName").value("서울특별시"))
				.andExpect(jsonPath("$.data.plans[0].dayCount").value(2))
				.andExpect(jsonPath("$.data.plans[0].likeCount").value(2))
				.andExpect(jsonPath("$.data.plans[0].viewCount").value(41));
	}

	@Test
	void returnsAnEmptyPageWhenTheRequestedPageIsPastTheLastPage() throws Exception {
		mockMvc.perform(get("/api/plans").queryParam("page", "2").queryParam("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.page").value(2))
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.hasNext").value(false))
				.andExpect(jsonPath("$.data.plans").isEmpty());
	}

	@Test
	void searchesByTrimmedRegionKeyword() throws Exception {
		mockMvc.perform(get("/api/plans").queryParam("keyword", "  서울  "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.keyword").value("서울"))
				.andExpect(jsonPath("$.data.totalCount").value(1));

		mockMvc.perform(get("/api/plans").queryParam("keyword", "부산"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalCount").value(0));
	}

	@Test
	void returnsPublicPlanDetailAndIncrementsViewCount() throws Exception {
		mockMvc.perform(get("/api/plans/{planId}", PUBLIC_PLAN_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.plan.planId").value(Long.toString(PUBLIC_PLAN_ID)))
				.andExpect(jsonPath("$.data.plan.viewCount").value(42))
				.andExpect(jsonPath("$.data.days[0].dayNo").value(1))
				.andExpect(jsonPath("$.data.days[0].items[0].placeName").value("경복궁"))
				.andExpect(jsonPath("$.data.days[0].items[0].timeSlot").value("MORNING"));

		Integer viewCount = jdbcTemplate.queryForObject(
				"SELECT VIEW_COUNT FROM TRAVEL_PLAN WHERE PLAN_ID = ?",
				Integer.class,
				PUBLIC_PLAN_ID
		);
		org.assertj.core.api.Assertions.assertThat(viewCount).isEqualTo(42);
	}

	@Test
	void hidesPrivatePlanAndRejectsInvalidId() throws Exception {
		mockMvc.perform(get("/api/plans/{planId}", PRIVATE_PLAN_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
		mockMvc.perform(get("/api/plans/not-a-number"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PATH_PARAMETER"));
	}

	private void deleteFixtures() {
		jdbcTemplate.update("DELETE FROM PLAN_LIKE WHERE PLAN_ID IN (?, ?)", PUBLIC_PLAN_ID, PRIVATE_PLAN_ID);
		jdbcTemplate.update("DELETE FROM PLAN_SCHEDULE_ITEM WHERE PLAN_DAY_ID IN (?, ?)", FIRST_DAY_ID, SECOND_DAY_ID);
		jdbcTemplate.update("DELETE FROM PLAN_DAY WHERE PLAN_ID IN (?, ?)", PUBLIC_PLAN_ID, PRIVATE_PLAN_ID);
		jdbcTemplate.update("DELETE FROM PLAN_MEMBER WHERE PLAN_ID IN (?, ?)", PUBLIC_PLAN_ID, PRIVATE_PLAN_ID);
		jdbcTemplate.update("DELETE FROM TRAVEL_PLAN WHERE PLAN_ID IN (?, ?)", PUBLIC_PLAN_ID, PRIVATE_PLAN_ID);
	}
}
