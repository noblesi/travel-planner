package com.noblesi.travelplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:travel_planner_member;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MemberControllerIntegrationTest {

    private static final String JOIN_EMAIL = "new.member@withtrip.test";
    private static final String RAW_PASSWORD = "WithTrip-2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM MEMBER WHERE EMAIL = ?", JOIN_EMAIL);
    }

    @Test
    void joinsMemberWithNormalizedDataAndEncodedPassword() throws Exception {
        mockMvc.perform(post("/api/users/join")
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinRequest("  NEW.MEMBER@WITHTRIP.TEST  ", RAW_PASSWORD, "Y")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));

        MemberRow member = jdbcTemplate.queryForObject(
                "SELECT EMAIL, MEMBER_NAME, BIRTH_DATE, GENDER_CODE, PHONE_NUMBER, PASSWORD_HASH, PRIVACY_CONSENT_YN "
                        + "FROM MEMBER WHERE EMAIL = ?",
                (resultSet, rowNumber) -> new MemberRow(
                        resultSet.getString("EMAIL"),
                        resultSet.getString("MEMBER_NAME"),
                        resultSet.getObject("BIRTH_DATE", LocalDate.class),
                        resultSet.getString("GENDER_CODE"),
                        resultSet.getString("PHONE_NUMBER"),
                        resultSet.getString("PASSWORD_HASH"),
                        resultSet.getString("PRIVACY_CONSENT_YN")
                ),
                JOIN_EMAIL
        );

        assertThat(member.email()).isEqualTo(JOIN_EMAIL);
        assertThat(member.name()).isEqualTo("홍길동");
        assertThat(member.birth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(member.gender()).isEqualTo("F");
        assertThat(member.phone()).isEqualTo("010-1234-5678");
        assertThat(passwordEncoder.matches(RAW_PASSWORD, member.passwordHash())).isTrue();
        assertThat(member.privacy()).isEqualTo("Y");
    }

    @Test
    void rejectsInvalidPasswordAndMissingPrivacyConsent() throws Exception {
        mockMvc.perform(post("/api/users/join")
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinRequest(JOIN_EMAIL, "short", "N")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        mockMvc.perform(post("/api/users/join")
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinRequest("E2E.OWNER@WITHTRIP.TEST", RAW_PASSWORD, "Y")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_MEMBER_EMAIL"));
    }

    @Test
    void checksExistingEmailAfterNormalization() throws Exception {
        mockMvc.perform(get("/api/users/emailCheck")
                        .param("email", "  E2E.OWNER@WITHTRIP.TEST  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    private String joinRequest(String email, String password, String privacy) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "name": "  홍길동  ",
                  "gender": "f",
                  "birth": "20000101",
                  "privacy": "%s",
                  "phone": "  010-1234-5678  "
                }
                """.formatted(email, password, privacy);
    }

    private record MemberRow(
            String email,
            String name,
            LocalDate birth,
            String gender,
            String phone,
            String passwordHash,
            String privacy
    ) {
    }
}
