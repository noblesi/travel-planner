package com.noblesi.travelplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_plan_search;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PlanSearchControllerIntegrationTest {
	private static final long PUBLISHED_PLAN_ID = 81_001L;
	private static final long DRAFT_PLAN_ID = 81_002L;
	private static final String MEMBER_EMAIL = "e2e.owner@withtrip.test";
	private static final String MEMBER_PASSWORD = "WithTrip-E2E-2026!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteFixtures();
		Long ownerMemberId = jdbcTemplate.queryForObject(
				"SELECT MEMBER_ID FROM MEMBER WHERE EMAIL = 'e2e.invitee@withtrip.test'",
				Long.class
		);

		insertPlan(PUBLISHED_PLAN_ID, ownerMemberId, "Published plan", "PUBLISHED", 11);
		insertPlan(DRAFT_PLAN_ID, ownerMemberId, "Draft plan", "DRAFT", 7);
	}

	@AfterEach
	void cleanUp() {
		deleteFixtures();
	}

	@Test
	void exposesOnlyPublishedPlansInSearchAndDetail() throws Exception {
		mockMvc.perform(get("/api/plan-search/plans"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.pagination.totalCount").value(1))
				.andExpect(jsonPath("$.data.content[0].planId").value(Long.toString(PUBLISHED_PLAN_ID)))
				.andExpect(jsonPath("$.data.content[0].title").value("Published plan"));

		mockMvc.perform(get("/api/plan-search/plans/{planId}", DRAFT_PLAN_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));

		Integer draftViewCount = jdbcTemplate.queryForObject(
				"SELECT VIEW_COUNT FROM TRAVEL_PLAN WHERE PLAN_ID = ?",
				Integer.class,
				DRAFT_PLAN_ID
		);
		assertThat(draftViewCount).isEqualTo(7);
	}

	@Test
	void rejectsCopyingDraftPlan() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(post("/api/plan-search/plans/{planId}/copy", DRAFT_PLAN_ID)
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "Copied plan",
						  "startDate": "2026-08-10",
						  "endDate": "2026-08-11"
						}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
	}

	@Test
	void storesReportWithLocalSchema() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(post("/api/plan-search/plans/{planId}/report", PUBLISHED_PLAN_ID)
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "reason": "FALSE_INFO",
						  "detail": "Integration test report"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		Integer reportCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM REPORT WHERE PLAN_ID = ? AND REASON_CODE = 'FALSE_INFO'",
				Integer.class,
				PUBLISHED_PLAN_ID
		);
		assertThat(reportCount).isEqualTo(1);
	}

	private void insertPlan(long planId, Long ownerMemberId, String title, String publishStatus, int viewCount) {
		jdbcTemplate.update("""
				INSERT INTO TRAVEL_PLAN (
				    PLAN_ID, OWNER_MEMBER_ID, TITLE, REGION_CODE, START_DATE, END_DATE,
				    VISIBILITY, PUBLISH_STATUS, PLAN_STATUS, VERSION_NO, VIEW_COUNT
				) VALUES (?, ?, ?, '1', DATE '2026-08-10', DATE '2026-08-11',
				          'PUBLIC', ?, 'ACTIVE', 0, ?)
				""", planId, ownerMemberId, title, publishStatus, viewCount);
	}

	private MockHttpSession login() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "%s",
						  "password": "%s"
						}
						""".formatted(MEMBER_EMAIL, MEMBER_PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private void deleteFixtures() {
		jdbcTemplate.update("DELETE FROM REPORT WHERE PLAN_ID IN (?, ?)", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
		jdbcTemplate.update("DELETE FROM PLAN_SCHEDULE_ITEM WHERE PLAN_DAY_ID IN (SELECT PLAN_DAY_ID FROM PLAN_DAY WHERE PLAN_ID IN (?, ?))", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
		jdbcTemplate.update("DELETE FROM PLAN_DAY WHERE PLAN_ID IN (?, ?)", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
		jdbcTemplate.update("DELETE FROM PLAN_MEMBER WHERE PLAN_ID IN (?, ?)", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
		jdbcTemplate.update("DELETE FROM PLAN_LIKE WHERE PLAN_ID IN (?, ?)", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
		jdbcTemplate.update("DELETE FROM TRAVEL_PLAN WHERE PLAN_ID IN (?, ?)", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
	}
}
