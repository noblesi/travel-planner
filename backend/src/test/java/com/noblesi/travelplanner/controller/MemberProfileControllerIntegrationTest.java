package com.noblesi.travelplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.noblesi.travelplanner.security.MemberPrincipal;
import com.noblesi.travelplanner.service.MemberProfileService;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_member_profile;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.auth.enforce-security=true",
		"app.profile.upload-path=build/test-profile-uploads"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MemberProfileControllerIntegrationTest {
	private static final String CURRENT_PASSWORD = "WithTrip-E2E-2026!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MemberProfileService memberProfileService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void resetOwnerProfile() {
		jdbcTemplate.update("""
				UPDATE MEMBER
				   SET MEMBER_NAME = '소유자',
				       NICKNAME = 'E2E 플랜 소유자',
				       BIRTH_DATE = NULL,
				       GENDER_CODE = NULL,
				       PHONE_NUMBER = NULL,
				       PROFILE_IMAGE_URL = NULL,
				       PASSWORD_HASH = ?,
				       MEMBER_STATUS = 'ACTIVE',
				       WITHDRAWN_AT = NULL,
				       UPDATED_AT = CURRENT_TIMESTAMP
				 WHERE MEMBER_ID = 1
				""", passwordEncoder.encode(CURRENT_PASSWORD));
	}

	@Test
	void rejectsAnonymousProfileRequest() throws Exception {
		mockMvc.perform(get("/api/members/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("CURRENT_MEMBER_NOT_AVAILABLE"));
	}

	@Test
	void returnsOnlyAuthenticatedMembersProfile() throws Exception {
		mockMvc.perform(get("/api/members/me").with(authentication(memberToken(1L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.memberId").value("1"))
				.andExpect(jsonPath("$.data.name").value("소유자"))
				.andExpect(jsonPath("$.data.email").value("e2e.owner@withtrip.test"))
				.andExpect(jsonPath("$.data.nickname").value("E2E 플랜 소유자"))
				.andExpect(jsonPath("$.data.memberStatus").doesNotExist())
				.andExpect(jsonPath("$.data.createdAt").doesNotExist())
				.andExpect(jsonPath("$.data.withdrawnAt").doesNotExist());
	}

	@Test
	void updatesOnlyAuthenticatedMembersEditableProfileFields() throws Exception {
		String inviteeNickname = jdbcTemplate.queryForObject(
				"SELECT NICKNAME FROM MEMBER WHERE MEMBER_ID = 2",
				String.class
		);

		mockMvc.perform(patch("/api/members/me")
						.with(authentication(memberToken(1L)))
						.with(csrf().asHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "  김여행  ",
								  "nickname": "  주말여행자  ",
								  "genderCode": "f",
								  "birthDate": "1998-04-02",
								  "phoneNumber": "  010-1234-5678  "
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.memberId").value("1"))
				.andExpect(jsonPath("$.data.name").value("김여행"))
				.andExpect(jsonPath("$.data.nickname").value("주말여행자"))
				.andExpect(jsonPath("$.data.genderCode").value("F"))
				.andExpect(jsonPath("$.data.birthDate").value("1998-04-02"))
				.andExpect(jsonPath("$.data.phoneNumber").value("010-1234-5678"))
				.andExpect(jsonPath("$.data.email").value("e2e.owner@withtrip.test"));

		assertThat(jdbcTemplate.queryForObject(
				"SELECT NICKNAME FROM MEMBER WHERE MEMBER_ID = 2",
				String.class
		)).isEqualTo(inviteeNickname);
	}

	@Test
	void rejectsInvalidProfileUpdate() throws Exception {
		mockMvc.perform(patch("/api/members/me")
						.with(authentication(memberToken(1L)))
						.with(csrf().asHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": " ",
								  "nickname": "여행자",
								  "genderCode": "unknown",
								  "birthDate": "2999-01-01",
								  "phoneNumber": "not-a-phone"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void rejectsAnonymousAndWithdrawnMemberUpdates() throws Exception {
		String request = """
				{
				  "name": "회원",
				  "nickname": "여행자",
				  "genderCode": "N",
				  "birthDate": null,
				  "phoneNumber": null
				}
				""";

		mockMvc.perform(patch("/api/members/me")
						.with(csrf().asHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(patch("/api/members/me")
						.with(authentication(memberToken(3L)))
						.with(csrf().asHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("CURRENT_MEMBER_NOT_AVAILABLE"));
	}

	@Test
	void withdrawsAuthenticatedMemberAfterCurrentPasswordVerification() throws Exception {
		mockMvc.perform(delete("/api/members/me")
						.with(authentication(memberToken(1L)))
						.with(csrf().asHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "currentPassword": "WithTrip-E2E-2026!" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(jdbcTemplate.queryForObject(
				"SELECT MEMBER_STATUS FROM MEMBER WHERE MEMBER_ID = 1",
				String.class
		)).isEqualTo("WITHDRAWN");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT WITHDRAWN_AT FROM MEMBER WHERE MEMBER_ID = 1",
				Object.class
		)).isNotNull();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT MEMBER_STATUS FROM MEMBER WHERE MEMBER_ID = 2",
				String.class
		)).isEqualTo("ACTIVE");
	}

	@Test
	void withdrawalClearsAndDeletesStoredProfileImageAfterCommit() throws Exception {
		Path uploadRoot = Path.of("build/test-profile-uploads");
		Path storedImage = uploadRoot.resolve("withdrawal-profile.png");
		Files.createDirectories(uploadRoot);
		Files.write(storedImage, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47});
		jdbcTemplate.update(
				"UPDATE MEMBER SET PROFILE_IMAGE_URL = ? WHERE MEMBER_ID = 1",
				"/uploads/profile/withdrawal-profile.png"
		);

		try {
			mockMvc.perform(delete("/api/members/me")
						.with(authentication(memberToken(1L)))
						.with(csrf().asHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "currentPassword": "WithTrip-E2E-2026!" }
								"""))
					.andExpect(status().isOk());

			assertThat(jdbcTemplate.queryForObject(
					"SELECT PROFILE_IMAGE_URL FROM MEMBER WHERE MEMBER_ID = 1",
					String.class
			)).isNull();
			assertThat(storedImage).doesNotExist();
		} finally {
			Files.deleteIfExists(storedImage);
		}
	}

	@Test
	void rejectsWithdrawalWhenCurrentPasswordDoesNotMatch() throws Exception {
		mockMvc.perform(delete("/api/members/me")
						.with(authentication(memberToken(1L)))
						.with(csrf().asHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "currentPassword": "wrong-password" }
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

		assertThat(jdbcTemplate.queryForObject(
				"SELECT MEMBER_STATUS FROM MEMBER WHERE MEMBER_ID = 1",
				String.class
		)).isEqualTo("ACTIVE");
	}

	@Test
	void changesPasswordAfterCurrentPasswordVerification() throws Exception {
		String newPassword = "New-WithTrip-2026!";

		mockMvc.perform(patch("/api/members/me/password")
						.with(authentication(memberToken(1L)))
						.with(csrf().asHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "WithTrip-E2E-2026!",
								  "newPassword": "New-WithTrip-2026!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		String passwordHash = jdbcTemplate.queryForObject(
				"SELECT PASSWORD_HASH FROM MEMBER WHERE MEMBER_ID = 1",
				String.class
		);
		assertThat(passwordEncoder.matches(newPassword, passwordHash)).isTrue();
		assertThat(passwordEncoder.matches(CURRENT_PASSWORD, passwordHash)).isFalse();
	}

	@Test
	void rejectsSameOrIncorrectPasswordChange() throws Exception {
		mockMvc.perform(patch("/api/members/me/password")
						.with(authentication(memberToken(1L)))
						.with(csrf().asHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "wrong-password",
								  "newPassword": "New-WithTrip-2026!"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

		mockMvc.perform(patch("/api/members/me/password")
						.with(authentication(memberToken(1L)))
						.with(csrf().asHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "WithTrip-E2E-2026!",
								  "newPassword": "WithTrip-E2E-2026!"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PASSWORD_UNCHANGED"));
	}

	@Test
	void uploadsValidatedProfileImageForAuthenticatedMember() throws Exception {
		byte[] png = {
				(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
				0x00, 0x00, 0x00, 0x00
		};
		MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);

		MvcResult result = mockMvc.perform(multipart(HttpMethod.PATCH, "/api/members/me/profile-image")
					.file(file)
					.with(authentication(memberToken(1L)))
					.with(csrf().asHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.profileImageUrl").value(org.hamcrest.Matchers.matchesPattern(
						"/uploads/profile/[a-f0-9]{32}\\.png"
				)))
				.andReturn();

		String profileImageUrl = jdbcTemplate.queryForObject(
				"SELECT PROFILE_IMAGE_URL FROM MEMBER WHERE MEMBER_ID = 1",
				String.class
		);
		assertThat(profileImageUrl).startsWith("/uploads/profile/").endsWith(".png");
		Path savedFile = Path.of("build/test-profile-uploads")
				.resolve(profileImageUrl.substring("/uploads/profile/".length()));
		assertThat(Files.readAllBytes(savedFile)).isEqualTo(png);
		Files.deleteIfExists(savedFile);
	}

	@Test
	void replacingProfileImageDeletesPreviousFileAfterCommit() throws Exception {
		Path uploadRoot = Path.of("build/test-profile-uploads");
		Path previousImage = uploadRoot.resolve("previous-profile.png");
		Files.createDirectories(uploadRoot);
		Files.write(previousImage, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47});
		jdbcTemplate.update(
				"UPDATE MEMBER SET PROFILE_IMAGE_URL = ? WHERE MEMBER_ID = 1",
				"/uploads/profile/previous-profile.png"
		);

		byte[] png = {
				(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
				0x00, 0x00, 0x00, 0x00
		};
		MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);
		Path newImage = null;

		try {
			mockMvc.perform(multipart(HttpMethod.PATCH, "/api/members/me/profile-image")
						.file(file)
						.with(authentication(memberToken(1L)))
						.with(csrf().asHeader()))
					.andExpect(status().isOk());

			String newImageUrl = jdbcTemplate.queryForObject(
					"SELECT PROFILE_IMAGE_URL FROM MEMBER WHERE MEMBER_ID = 1",
					String.class
			);
			newImage = uploadRoot.resolve(newImageUrl.substring("/uploads/profile/".length()));
			assertThat(previousImage).doesNotExist();
			assertThat(newImage).exists();
		} finally {
			Files.deleteIfExists(previousImage);
			if (newImage != null) {
				Files.deleteIfExists(newImage);
			}
		}
	}

	@Test
	void rollingBackProfileImageReplacementKeepsPreviousFileAndDeletesNewFile() throws Exception {
		Path uploadRoot = Path.of("build/test-profile-uploads");
		Path previousImage = uploadRoot.resolve("rollback-previous-profile.png");
		Files.createDirectories(uploadRoot);
		Files.write(previousImage, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47});
		String previousImageUrl = "/uploads/profile/rollback-previous-profile.png";
		jdbcTemplate.update(
				"UPDATE MEMBER SET PROFILE_IMAGE_URL = ? WHERE MEMBER_ID = 1",
				previousImageUrl
		);

		byte[] png = {
				(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
				0x00, 0x00, 0x00, 0x00
		};
		MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);
		AtomicReference<String> newImageUrl = new AtomicReference<>();
		Path newImage = null;

		try {
			new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
				newImageUrl.set(memberProfileService.updateProfileImage(file).profileImageUrl());
				status.setRollbackOnly();
			});

			newImage = uploadRoot.resolve(
					newImageUrl.get().substring("/uploads/profile/".length())
			);
			assertThat(jdbcTemplate.queryForObject(
					"SELECT PROFILE_IMAGE_URL FROM MEMBER WHERE MEMBER_ID = 1",
					String.class
			)).isEqualTo(previousImageUrl);
			assertThat(previousImage).exists();
			assertThat(newImage).doesNotExist();
		} finally {
			Files.deleteIfExists(previousImage);
			if (newImage != null) {
				Files.deleteIfExists(newImage);
			}
		}
	}

	@Test
	void rejectsAnonymousOrInvalidProfileImageUpload() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
				"file",
				"avatar.png",
				"image/png",
				new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
		);
		mockMvc.perform(multipart(HttpMethod.PATCH, "/api/members/me/profile-image")
					.file(image)
					.with(csrf().asHeader()))
				.andExpect(status().isUnauthorized());

		MockMultipartFile textFile = new MockMultipartFile(
				"file",
				"avatar.png",
				"image/png",
				"not-an-image".getBytes()
		);
		mockMvc.perform(multipart(HttpMethod.PATCH, "/api/members/me/profile-image")
					.file(textFile)
					.with(authentication(memberToken(1L)))
					.with(csrf().asHeader()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PROFILE_IMAGE"));
	}

	private UsernamePasswordAuthenticationToken memberToken(long memberId) {
		MemberPrincipal principal = new MemberPrincipal(
				memberId,
				"member%s@withtrip.test".formatted(memberId),
				"회원%s".formatted(memberId)
		);
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
	}
}
