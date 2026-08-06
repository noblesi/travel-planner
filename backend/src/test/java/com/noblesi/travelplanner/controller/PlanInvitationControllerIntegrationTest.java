package com.noblesi.travelplanner.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.HexFormat;

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
class PlanInvitationControllerIntegrationTest {

	private static final long PLAN_ID = 8_001L;
	private static final long INVITATION_ID = 8_101L;
	private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEFGH123456789";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	@BeforeEach
	void setUp() {
		deletePlanData();
	}

	@AfterEach
	void cleanUp() {
		deletePlanData();
	}

	@Test
	void createsNormalizedInvitationLinksForOwnedPlan() throws Exception {
		insertPlan(PLAN_ID, 1L);

		mockMvc.perform(post("/api/plans/{planId}/invitations", PLAN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "inviteeEmails": [
							    " Friend@Example.com ",
							    "friend@example.com",
							    "second@example.com"
							  ]
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.planId").value(Long.toString(PLAN_ID)))
				.andExpect(jsonPath("$.data.invitations", hasSize(2)))
				.andExpect(jsonPath("$.data.invitations[0].inviteeEmail")
						.value("friend@example.com"))
				.andExpect(jsonPath("$.data.invitations[0].token")
						.value(matchesPattern("[A-Za-z0-9_-]{43}")));

		Integer invitationCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM PLAN_INVITATION WHERE PLAN_ID = ?",
				Integer.class,
				PLAN_ID
		);
		Integer hashedTokenCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM PLAN_INVITATION WHERE PLAN_ID = ? AND LENGTH(INVITATION_TOKEN) = 64",
				Integer.class,
				PLAN_ID
		);
		org.assertj.core.api.Assertions.assertThat(invitationCount).isEqualTo(2);
		org.assertj.core.api.Assertions.assertThat(hashedTokenCount).isEqualTo(2);
	}

	@Test
	void reissuingInvitationCancelsPreviousPendingLink() throws Exception {
		insertPlan(PLAN_ID, 1L);
		String body = """
				{
				  "inviteeEmails": ["friend@example.com"]
				}
				""";

		mockMvc.perform(post("/api/plans/{planId}/invitations", PLAN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/plans/{planId}/invitations", PLAN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isCreated());

		Integer canceledCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM PLAN_INVITATION WHERE PLAN_ID = ? AND INVITATION_STATUS = 'CANCELED'",
				Integer.class,
				PLAN_ID
		);
		Integer pendingCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM PLAN_INVITATION WHERE PLAN_ID = ? AND INVITATION_STATUS = 'PENDING'",
				Integer.class,
				PLAN_ID
		);
		org.assertj.core.api.Assertions.assertThat(canceledCount).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(pendingCount).isEqualTo(1);
	}

	@Test
	void returnsPendingInvitationDetailsByToken() throws Exception {
		insertPlan(PLAN_ID, 2L);
		insertInvitation(INVITATION_ID, PLAN_ID, 2L, TOKEN, "PENDING", null, futureExpiry());

		mockMvc.perform(get("/api/plan-invitations/{token}", TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.invitationId").value(Long.toString(INVITATION_ID)))
				.andExpect(jsonPath("$.data.planId").value(Long.toString(PLAN_ID)))
				.andExpect(jsonPath("$.data.planTitle").value("서울특별시 여행"))
				.andExpect(jsonPath("$.data.regionName").value("서울특별시"))
				.andExpect(jsonPath("$.data.inviteeEmail").value("friend@example.com"))
				.andExpect(jsonPath("$.data.status").value("PENDING"));
	}

	@Test
	void acceptsInvitationAndGrantsScheduleEditorAccess() throws Exception {
		insertPlan(PLAN_ID, 2L);
		insertInvitation(INVITATION_ID, PLAN_ID, 2L, TOKEN, "PENDING", null, futureExpiry());

		mockMvc.perform(post("/api/plan-invitations/{token}/accept", TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ACCEPTED"))
				.andExpect(jsonPath("$.data.acceptedMemberId").value("1"));

		mockMvc.perform(get("/api/plans/{planId}/editor", PLAN_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.plan.planId").value(Long.toString(PLAN_ID)));

		String participantType = jdbcTemplate.queryForObject(
				"SELECT PARTICIPANT_TYPE FROM PLAN_MEMBER WHERE PLAN_ID = ? AND MEMBER_ID = 1",
				String.class,
				PLAN_ID
		);
		org.assertj.core.api.Assertions.assertThat(participantType).isEqualTo("INVITEE");
	}

	@Test
	void acceptingSameInvitationTwiceIsIdempotentForSameMember() throws Exception {
		insertPlan(PLAN_ID, 2L);
		insertInvitation(INVITATION_ID, PLAN_ID, 2L, TOKEN, "PENDING", null, futureExpiry());

		mockMvc.perform(post("/api/plan-invitations/{token}/accept", TOKEN))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/plan-invitations/{token}/accept", TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ACCEPTED"));

		Integer memberCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM PLAN_MEMBER WHERE PLAN_ID = ? AND MEMBER_ID = 1",
				Integer.class,
				PLAN_ID
		);
		org.assertj.core.api.Assertions.assertThat(memberCount).isEqualTo(1);
	}

	@Test
	void keepsMetadataAndDateChangesOwnerOnlyAfterInvitationAcceptance() throws Exception {
		insertPlan(PLAN_ID, 2L);
		insertInvitation(INVITATION_ID, PLAN_ID, 2L, TOKEN, "PENDING", null, futureExpiry());
		mockMvc.perform(post("/api/plan-invitations/{token}/accept", TOKEN))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/plans/{planId}", PLAN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title": "권한 없는 변경",
							  "visibility": "PUBLIC",
							  "versionNo": 0
							}
							"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
	}

	@Test
	void rejectsExpiredAndInvalidInvitationTokens() throws Exception {
		insertPlan(PLAN_ID, 2L);
		OffsetDateTime createdAt = now().minusDays(2);
		insertInvitation(
				INVITATION_ID,
				PLAN_ID,
				2L,
				TOKEN,
				"PENDING",
				createdAt,
				createdAt.plusHours(24)
		);

		mockMvc.perform(get("/api/plan-invitations/{token}", TOKEN))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("INVITATION_EXPIRED"));
		mockMvc.perform(get("/api/plan-invitations/invalid"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
	}

	@Test
	void rejectsSelfAcceptanceAndInvalidInvitationRequests() throws Exception {
		insertPlan(PLAN_ID, 1L);
		insertInvitation(INVITATION_ID, PLAN_ID, 1L, TOKEN, "PENDING", null, futureExpiry());

		mockMvc.perform(post("/api/plan-invitations/{token}/accept", TOKEN))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVITATION_SELF_ACCEPTANCE_NOT_ALLOWED"));

		mockMvc.perform(post("/api/plans/{planId}/invitations", PLAN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "inviteeEmails": ["not-an-email"]
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	private void insertPlan(long planId, long ownerMemberId) {
		jdbcTemplate.update("""
				INSERT INTO TRAVEL_PLAN (
				    PLAN_ID, OWNER_MEMBER_ID, TITLE, REGION_CODE,
				    START_DATE, END_DATE, VISIBILITY, PLAN_STATUS, VERSION_NO
				) VALUES (?, ?, '서울특별시 여행', '1', DATE '2026-08-10',
				          DATE '2026-08-11', 'PRIVATE', 'ACTIVE', 0)
				""", planId, ownerMemberId);
	}

	private void insertInvitation(
			long invitationId,
			long planId,
			long inviterMemberId,
			String token,
			String status,
			OffsetDateTime createdAt,
			OffsetDateTime expiresAt
	) {
		OffsetDateTime effectiveCreatedAt = createdAt == null
				? now()
				: createdAt;
		jdbcTemplate.update("""
				INSERT INTO PLAN_INVITATION (
				    INVITATION_ID, PLAN_ID, INVITER_MEMBER_ID, INVITEE_EMAIL,
				    INVITATION_STATUS, INVITATION_TOKEN, EXPIRES_AT, CREATED_AT
				) VALUES (?, ?, ?, 'friend@example.com', ?, ?, ?, ?)
				""",
				invitationId,
				planId,
				inviterMemberId,
				status,
				hashToken(token),
				expiresAt,
				effectiveCreatedAt
		);
	}

	private OffsetDateTime futureExpiry() {
		return now().plusHours(24);
	}

	private OffsetDateTime now() {
		return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
	}

	private String hashToken(String token) {
		try {
			return HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256")
							.digest(token.getBytes(StandardCharsets.UTF_8))
			);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private void deletePlanData() {
		jdbcTemplate.update("DELETE FROM PLAN_INVITATION");
		jdbcTemplate.update("DELETE FROM PLAN_EDIT_OPERATION");
		jdbcTemplate.update("DELETE FROM PLAN_SCHEDULE_ITEM");
		jdbcTemplate.update("DELETE FROM PLAN_DAY");
		jdbcTemplate.update("DELETE FROM PLAN_MEMBER");
		jdbcTemplate.update("DELETE FROM TRAVEL_PLAN");
	}
}
