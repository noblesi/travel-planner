package com.noblesi.travelplanner.admin.trip.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.noblesi.travelplanner.admin.trip.dto.AdminRecommendRuleDTO;
import com.noblesi.travelplanner.admin.trip.service.AdminRecommendRuleService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:travel_planner_recommend_rule;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminRecommendRuleControllerIntegrationTest {

    private static final String TEST_LOGIN_ID = "admin1";
    private static final String TEST_PASSWORD = "test1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminRecommendRuleService adminRecommendRuleService;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM RECOMMEND_RULE");
    }

    @Test
    void rendersDefaultRuleWhenNoActiveRuleExists() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/trips").session(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/trip/tripFormView"))
                .andReturn();

        AdminRecommendRuleDTO rule = (AdminRecommendRuleDTO) result.getModelAndView()
                .getModel()
                .get("recommendRule");
        assertThat(rule.getLikeWeight()).isEqualByComparingTo("40");
        assertThat(rule.getViewWeight()).isEqualByComparingTo("20");
        assertThat(rule.getCopyWeight()).isEqualByComparingTo("40");
    }

    @Test
    void savesOneActiveRuleForAuthenticatedAdmin() throws Exception {
        mockMvc.perform(post("/admin/trips/recommend-rule")
                        .session(login())
                        .with(csrf())
                        .param("likeWeight", "50")
                        .param("viewWeight", "30")
                        .param("copyWeight", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/trips"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RECOMMEND_RULE WHERE ACTIVE_YN = 'Y'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT LIKE_WEIGHT FROM RECOMMEND_RULE WHERE ACTIVE_YN = 'Y'",
                BigDecimal.class
        )).isEqualByComparingTo("50");
    }

    @Test
    void rejectsWeightsWhoseSumIsNotOneHundred() throws Exception {
        mockMvc.perform(post("/admin/trips/recommend-rule")
                        .session(login())
                        .with(csrf())
                        .param("likeWeight", "50")
                        .param("viewWeight", "30")
                        .param("copyWeight", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RECOMMEND_WEIGHT"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RECOMMEND_RULE",
                Integer.class
        )).isZero();
    }

    @Test
    void restoresPreviousActiveRuleWhenNewRuleInsertFails() {
        jdbcTemplate.update("""
                INSERT INTO RECOMMEND_RULE (
                    RULE_ID, LIKE_WEIGHT, VIEW_WEIGHT, COPY_WEIGHT, ACTIVE_YN, ADMIN_ID
                ) VALUES (
                    SEQ_RECOMMEND_RULE.NEXTVAL, 40, 20, 40, 'Y', 1
                )
                """);

        AdminRecommendRuleDTO rule = new AdminRecommendRuleDTO();
        rule.setLikeWeight(new BigDecimal("50"));
        rule.setViewWeight(new BigDecimal("30"));
        rule.setCopyWeight(new BigDecimal("20"));

        assertThatThrownBy(() -> adminRecommendRuleService.saveRecommendRule(rule, 999999L))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RECOMMEND_RULE WHERE ACTIVE_YN = 'Y'",
                Integer.class
        )).isEqualTo(1);
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/login")
                        .with(csrf())
                        .param("loginId", TEST_LOGIN_ID)
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
