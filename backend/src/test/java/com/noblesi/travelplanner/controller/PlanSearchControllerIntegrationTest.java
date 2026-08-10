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
						  "startDate": "2099-08-10",
						  "endDate": "2099-08-11"
						}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
	}

	@Test
	void rejectsLikeAndReportForDraftPlan() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(post("/api/plan-search/plans/{planId}/like", DRAFT_PLAN_ID)
				.session(session)
				.with(csrf().asHeader()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));

		mockMvc.perform(post("/api/plan-search/plans/{planId}/report", DRAFT_PLAN_ID)
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(reportRequest("FALSE_INFO", "Draft report")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));

		Integer likeCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM PLAN_LIKE WHERE PLAN_ID = ?",
				Integer.class,
				DRAFT_PLAN_ID
		);
		Integer reportCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM REPORT WHERE PLAN_ID = ?",
				Integer.class,
				DRAFT_PLAN_ID
		);
		assertThat(likeCount).isZero();
		assertThat(reportCount).isZero();
	}

	@Test
	void storesOneReportAndRejectsDuplicate() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(post("/api/plan-search/plans/{planId}/report", PUBLISHED_PLAN_ID)
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(reportRequest("FALSE_INFO", "Integration test report")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(post("/api/plan-search/plans/{planId}/report", PUBLISHED_PLAN_ID)
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(reportRequest("SPAM", "Duplicate report")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("REPORT_ALREADY_EXISTS"));

		Integer reportCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM REPORT WHERE PLAN_ID = ? AND REASON_CODE = 'FALSE_INFO'",
				Integer.class,
				PUBLISHED_PLAN_ID
		);
		assertThat(reportCount).isEqualTo(1);
	}

	@Test
	void validatesReportReasonAndRejectsSelfReport() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(post("/api/plan-search/plans/{planId}/report", PUBLISHED_PLAN_ID)
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(reportRequest("UNKNOWN", "Invalid reason")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		Long currentMemberId = jdbcTemplate.queryForObject(
				"SELECT MEMBER_ID FROM MEMBER WHERE EMAIL = ?",
				Long.class,
				MEMBER_EMAIL
		);
		long selfOwnedPlanId = 81_003L;
		insertPlan(selfOwnedPlanId, currentMemberId, "Self owned plan", "PUBLISHED", 0);

		mockMvc.perform(post("/api/plan-search/plans/{planId}/report", selfOwnedPlanId)
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(reportRequest("OTHER", "Self report")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("SELF_PLAN_REPORT_NOT_ALLOWED"));
	}

	@Test
	void validatesCopyRequestAndCreatesExplicitDraftState() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(post("/api/plan-search/plans/{planId}/copy", PUBLISHED_PLAN_ID)
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "   ",
						  "startDate": "2099-08-10",
						  "endDate": "2099-08-11"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/plan-search/plans/{planId}/copy", PUBLISHED_PLAN_ID)
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "Too long trip",
						  "startDate": "2099-08-01",
						  "endDate": "2099-08-15"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TRAVEL_PLAN_DURATION_EXCEEDED"));

		mockMvc.perform(post("/api/plan-search/plans/{planId}/copy", PUBLISHED_PLAN_ID)
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "  Copied plan  ",
						  "startDate": "2099-08-10",
						  "endDate": "2099-08-11"
						}
						"""))
				.andExpect(status().isOk());

		String copiedState = jdbcTemplate.queryForObject("""
				SELECT TITLE || ':' || VISIBILITY || ':' || PUBLISH_STATUS || ':' || PLAN_STATUS
				  FROM TRAVEL_PLAN
				 WHERE SOURCE_PLAN_ID = ?
				""", String.class, PUBLISHED_PLAN_ID);
		assertThat(copiedState).isEqualTo("Copied plan:PRIVATE:DRAFT:ACTIVE");
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

	private String reportRequest(String reason, String detail) {
		return """
				{
				  "reason": "%s",
				  "detail": "%s"
				}
				""".formatted(reason, detail);
	}

	private void deleteFixtures() {
		// 복사 테스트가 생성한 동적 ID까지 제거해 테스트 순서와 무관한 격리를 보장한다.
		jdbcTemplate.update("DELETE FROM REPORT WHERE PLAN_ID IN (SELECT PLAN_ID FROM TRAVEL_PLAN WHERE SOURCE_PLAN_ID IN (?, ?))", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
		jdbcTemplate.update("DELETE FROM PLAN_SCHEDULE_ITEM WHERE PLAN_DAY_ID IN (SELECT PLAN_DAY_ID FROM PLAN_DAY WHERE PLAN_ID IN (SELECT PLAN_ID FROM TRAVEL_PLAN WHERE SOURCE_PLAN_ID IN (?, ?)))", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
		jdbcTemplate.update("DELETE FROM PLAN_DAY WHERE PLAN_ID IN (SELECT PLAN_ID FROM TRAVEL_PLAN WHERE SOURCE_PLAN_ID IN (?, ?))", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
		jdbcTemplate.update("DELETE FROM PLAN_MEMBER WHERE PLAN_ID IN (SELECT PLAN_ID FROM TRAVEL_PLAN WHERE SOURCE_PLAN_ID IN (?, ?))", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
		jdbcTemplate.update("DELETE FROM TRAVEL_PLAN WHERE SOURCE_PLAN_ID IN (?, ?)", PUBLISHED_PLAN_ID, DRAFT_PLAN_ID);
		jdbcTemplate.update("DELETE FROM REPORT WHERE PLAN_ID BETWEEN 81_001 AND 81_003");
		jdbcTemplate.update("DELETE FROM PLAN_SCHEDULE_ITEM WHERE PLAN_DAY_ID IN (SELECT PLAN_DAY_ID FROM PLAN_DAY WHERE PLAN_ID BETWEEN 81_001 AND 81_003)");
		jdbcTemplate.update("DELETE FROM PLAN_DAY WHERE PLAN_ID BETWEEN 81_001 AND 81_003");
		jdbcTemplate.update("DELETE FROM PLAN_MEMBER WHERE PLAN_ID BETWEEN 81_001 AND 81_003");
		jdbcTemplate.update("DELETE FROM PLAN_LIKE WHERE PLAN_ID BETWEEN 81_001 AND 81_003");
		jdbcTemplate.update("DELETE FROM TRAVEL_PLAN WHERE PLAN_ID BETWEEN 81_001 AND 81_003");
	}
}
