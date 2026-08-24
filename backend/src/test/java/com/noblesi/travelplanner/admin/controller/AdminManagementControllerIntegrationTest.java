package com.noblesi.travelplanner.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.admin.dashboard.dto.AdminDashboardDTO;
import com.noblesi.travelplanner.admin.member.dto.AdminMemberDetailDTO;
import com.noblesi.travelplanner.admin.member.dto.AdminMemberListDTO;
import com.noblesi.travelplanner.admin.report.dto.AdminReportDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripDetailDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripListDTO;
import com.noblesi.travelplanner.common.api.PageResponse;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:travel_planner_admin_management;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AdminManagementControllerIntegrationTest {

    private static final String TEST_LOGIN_ID = "admin1";
    private static final String TEST_PASSWORD = "test1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rendersDashboardMetricsForAuthenticatedAdmin() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/dashboard").session(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard/adminDashboardView"))
                .andExpect(model().attributeExists("dashboard"))
                .andReturn();

        AdminDashboardDTO dashboard = (AdminDashboardDTO) result.getModelAndView()
                .getModel()
                .get("dashboard");
        assertThat(dashboard.getTotalMemberCount()).isEqualTo(4);
        assertThat(dashboard.getWeeklyPlanStats()).hasSize(7);
        assertThat(dashboard.getPopularRegionStats()).isEmpty();
    }

    @Test
    void searchesMemberRendersDetailAndChangesStatus() throws Exception {
        Long memberId = jdbcTemplate.queryForObject(
                "SELECT MEMBER_ID FROM MEMBER WHERE EMAIL = ?",
                Long.class,
                "e2e.owner@withtrip.test"
        );
        MockHttpSession session = login();

        MvcResult listResult = mockMvc.perform(get("/admin/members")
                        .session(session)
                        .param("keyword", "e2e.owner")
                        .param("memberStatus", "active"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/member/memberFormView"))
                .andExpect(model().attribute("keyword", "e2e.owner"))
                .andExpect(model().attribute("memberStatus", "active"))
                .andReturn();

        @SuppressWarnings("unchecked")
        PageResponse<AdminMemberListDTO> members = (PageResponse<AdminMemberListDTO>) listResult.getModelAndView()
                .getModel()
                .get("members");
        assertThat(members.content()).extracting(AdminMemberListDTO::getEmail)
                .containsExactly("e2e.owner@withtrip.test");
        assertThat(members.pagination().page()).isEqualTo(1);
        assertThat(members.pagination().totalCount()).isEqualTo(1);

        MvcResult detailResult = mockMvc.perform(get("/admin/members/{memberId}", memberId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/member/memberDetailView"))
                .andReturn();
        AdminMemberDetailDTO member = (AdminMemberDetailDTO) detailResult.getModelAndView()
                .getModel()
                .get("member");
        assertThat(member.getEmail()).isEqualTo("e2e.owner@withtrip.test");
        assertThat(member.getMemberStatus()).isEqualTo("ACTIVE");

        mockMvc.perform(post("/admin/members/{memberId}/status", memberId)
                        .session(session)
                        .with(csrf())
                        .param("memberStatus", "withdrawn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/members/" + memberId))
                .andExpect(flash().attribute("message", "회원 상태가 변경되었습니다."));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT MEMBER_STATUS FROM MEMBER WHERE MEMBER_ID = ?",
                String.class,
                memberId
        )).isEqualTo("WITHDRAWN");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT WITHDRAWN_AT FROM MEMBER WHERE MEMBER_ID = ?",
                java.time.OffsetDateTime.class,
                memberId
        )).isNotNull();
    }

    @Test
    void filtersTripListAndRendersTripDetail() throws Exception {
        Long planId = createPlan("관리자 여행 통합 테스트", "PUBLIC");
        Long planDayId = createPlanDay(planId);
        jdbcTemplate.update("""
                INSERT INTO PLAN_SCHEDULE_ITEM (
                    SCHEDULE_ITEM_ID, PLAN_DAY_ID, TIME_SLOT, POSITION_NO,
                    PLACE_PROVIDER, EXTERNAL_PLACE_ID, PLACE_NAME_SNAPSHOT,
                    CATEGORY_SNAPSHOT, ADDRESS_SNAPSHOT, LATITUDE_SNAPSHOT, LONGITUDE_SNAPSHOT
                ) VALUES (
                    SEQ_PLAN_SCHEDULE_ITEM.NEXTVAL, ?, 'MORNING', 1,
                    'TOUR_API', 'admin-test-place', '테스트 장소',
                    '관광지', '서울특별시', 37.5665, 126.9780
                )
                """, planDayId);
        MockHttpSession session = login();

        MvcResult listResult = mockMvc.perform(get("/admin/trips")
                        .session(session)
                        .param("keyword", "관리자 여행")
                        .param("visibility", "public")
                        .param("regionCode", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/trip/tripFormView"))
                .andExpect(model().attribute("totalCount", 1))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<AdminTripListDTO> trips = (List<AdminTripListDTO>) listResult.getModelAndView()
                .getModel()
                .get("trips");
        assertThat(trips).extracting(AdminTripListDTO::getPlanId).containsExactly(planId);

        MvcResult detailResult = mockMvc.perform(get("/admin/trips/{planId}", planId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/trip/tripDetailView"))
                .andExpect(model().attributeExists("trip", "schedules", "scheduleDays", "reports"))
                .andReturn();
        AdminTripDetailDTO trip = (AdminTripDetailDTO) detailResult.getModelAndView()
                .getModel()
                .get("trip");
        assertThat(trip.getTitle()).isEqualTo("관리자 여행 통합 테스트");
        assertThat(detailResult.getModelAndView().getModel().get("scheduleDays")).isEqualTo(List.of(1));
    }

    @Test
    void rendersAndCompletesReportWhileHidingPlan() throws Exception {
        Long planId = createPlan("신고 처리 통합 테스트", "PUBLIC");
        Long reportId = createReport(planId);
        jdbcTemplate.update(
                "UPDATE TRAVEL_PLAN SET UPDATED_AT = TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00:00' WHERE PLAN_ID = ?",
                planId
        );
        MockHttpSession session = login();

        MvcResult detailResult = mockMvc.perform(get("/admin/reports/{reportId}", reportId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/report/reportDetailView"))
                .andExpect(model().attributeExists("report", "processForm"))
                .andReturn();
        AdminReportDTO report = (AdminReportDTO) detailResult.getModelAndView()
                .getModel()
                .get("report");
        assertThat(report.getReportStatus()).isEqualTo("PENDING");
        assertThat(report.getPlanId()).isEqualTo(planId);

        mockMvc.perform(post("/admin/reports/{reportId}/complete", reportId)
                        .session(session)
                        .with(csrf())
                        .param("processResultCode", "HIDDEN")
                        .param("processReason", "운영 정책 위반"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reports/" + reportId))
                .andExpect(flash().attribute("message", "신고 검토가 완료되었습니다."));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT REPORT_STATUS FROM REPORT WHERE REPORT_ID = ?",
                String.class,
                reportId
        )).isEqualTo("RESOLVED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT VISIBILITY FROM TRAVEL_PLAN WHERE PLAN_ID = ?",
                String.class,
                planId
        )).isEqualTo("PRIVATE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT VERSION_NO FROM TRAVEL_PLAN WHERE PLAN_ID = ?",
                Integer.class,
                planId
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT UPDATED_AT FROM TRAVEL_PLAN WHERE PLAN_ID = ?",
                java.time.OffsetDateTime.class,
                planId
        )).isAfter(java.time.OffsetDateTime.parse("2000-01-01T00:00:00Z"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT PROCESS_RESULT_CODE FROM REPORT_PROCESS WHERE REPORT_ID = ?",
                String.class,
                reportId
        )).isEqualTo("HIDDEN");
    }

    private Long createPlan(String title, String visibility) {
        LocalDate startDate = LocalDate.now().plusDays(1);
        jdbcTemplate.update("""
                INSERT INTO TRAVEL_PLAN (
                    PLAN_ID, OWNER_MEMBER_ID, TITLE, REGION_CODE,
                    START_DATE, END_DATE, VISIBILITY, PUBLISH_STATUS, PLAN_STATUS
                ) VALUES (
                    SEQ_TRAVEL_PLAN.NEXTVAL, 1, ?, '1', ?, ?, ?, 'PUBLISHED', 'ACTIVE'
                )
                """,
                title,
                Date.valueOf(startDate),
                Date.valueOf(startDate.plusDays(1)),
                visibility
        );
        Long planId = jdbcTemplate.queryForObject(
                "SELECT PLAN_ID FROM TRAVEL_PLAN WHERE TITLE = ?",
                Long.class,
                title
        );
        jdbcTemplate.update(
                "INSERT INTO PLAN_MEMBER (PLAN_ID, MEMBER_ID, PARTICIPANT_TYPE) VALUES (?, 1, 'CREATOR')",
                planId
        );
        return planId;
    }

    private Long createPlanDay(Long planId) {
        jdbcTemplate.update("""
                INSERT INTO PLAN_DAY (PLAN_DAY_ID, PLAN_ID, DAY_NO, TRAVEL_DATE)
                SELECT SEQ_PLAN_DAY.NEXTVAL, PLAN_ID, 1, START_DATE
                  FROM TRAVEL_PLAN
                 WHERE PLAN_ID = ?
                """, planId);
        return jdbcTemplate.queryForObject(
                "SELECT PLAN_DAY_ID FROM PLAN_DAY WHERE PLAN_ID = ? AND DAY_NO = 1",
                Long.class,
                planId
        );
    }

    private Long createReport(Long planId) {
        jdbcTemplate.update("""
                INSERT INTO REPORT (
                    REPORT_ID, PLAN_ID, REPORTER_MEMBER_ID,
                    REASON_CODE, REASON_DETAIL, REPORT_STATUS
                ) VALUES (
                    SEQ_REPORT.NEXTVAL, ?, 2, 'INAPPROPRIATE', '통합 테스트 신고', 'PENDING'
                )
                """, planId);
        return jdbcTemplate.queryForObject(
                "SELECT REPORT_ID FROM REPORT WHERE PLAN_ID = ? AND REPORTER_MEMBER_ID = 2",
                Long.class,
                planId
        );
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
