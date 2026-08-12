package com.noblesi.travelplanner.admin.notice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:travel_planner_admin_notice;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminNoticeControllerIntegrationTest {

    private static final String TEST_LOGIN_ID = "admin1";
    private static final String TEST_PASSWORD = "test1234";
    private static final String NOTICE_TITLE_PREFIX = "통합 테스트 공지";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM NOTICE WHERE TITLE LIKE ?", NOTICE_TITLE_PREFIX + "%");
    }

    @Test
    void redirectsAnonymousRequestToAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/notices"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void rendersNoticeListForAuthenticatedAdmin() throws Exception {
        mockMvc.perform(get("/admin/notices")
                        .session(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/notice/noticeFormView"))
                .andExpect(model().attributeExists("notices"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("공지사항 관리")));
    }

    @Test
    void validatesRequiredFieldsBeforeCreatingNotice() throws Exception {
        mockMvc.perform(post("/admin/notices")
                        .session(login())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/notice/noticeWriteView"))
                .andExpect(model().attributeHasFieldErrors("noticeForm", "title", "content", "categoryCode"));
    }

    @Test
    void createsUpdatesAndDeletesNotice() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/admin/notices")
                        .session(session)
                        .with(csrf())
                        .param("title", "  " + NOTICE_TITLE_PREFIX + " 등록  ")
                        .param("content", "  최초 내용  ")
                        .param("categoryCode", "guide"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/notices"));

        Long noticeId = jdbcTemplate.queryForObject(
                "SELECT NOTICE_ID FROM NOTICE WHERE TITLE = ?",
                Long.class,
                NOTICE_TITLE_PREFIX + " 등록"
        );
        assertThat(noticeId).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT CATEGORY_CODE FROM NOTICE WHERE NOTICE_ID = ?",
                String.class,
                noticeId
        )).isEqualTo("GUIDE");

        mockMvc.perform(post("/admin/notices/{noticeId}", noticeId)
                        .session(session)
                        .with(csrf())
                        .param("title", NOTICE_TITLE_PREFIX + " 수정")
                        .param("content", "수정된 내용")
                        .param("categoryCode", "MAINTENANCE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/notices/" + noticeId));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT TITLE FROM NOTICE WHERE NOTICE_ID = ?",
                String.class,
                noticeId
        )).isEqualTo(NOTICE_TITLE_PREFIX + " 수정");

        mockMvc.perform(post("/admin/notices/{noticeId}/delete", noticeId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/notices"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM NOTICE WHERE NOTICE_ID = ?",
                Integer.class,
                noticeId
        )).isZero();
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
