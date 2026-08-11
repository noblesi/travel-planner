package com.noblesi.travelplanner.notice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_notice;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class NoticeControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void resetViewCounts() {
		jdbcTemplate.update("UPDATE NOTICE SET VIEW_COUNT = 0");
	}

	@Test
	void returnsVisibleNoticesWithPaginationAndCategoryFilter() throws Exception {
		mockMvc.perform(get("/api/notices")
					.param("page", "1")
					.param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content.length()").value(2))
				.andExpect(jsonPath("$.data.content[0].noticeId").value(3))
				.andExpect(jsonPath("$.data.content[0].category").value("MAINTENANCE"))
				.andExpect(jsonPath("$.data.pagination.totalCount").value(3))
				.andExpect(jsonPath("$.data.pagination.totalPages").value(2));

		mockMvc.perform(get("/api/notices")
					.param("category", "GUIDE")
					.param("page", "1")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content.length()").value(2))
				.andExpect(jsonPath("$.data.pagination.totalCount").value(2));
	}

	@Test
	void returnsDetailAndIncrementsViewCount() throws Exception {
		Integer before = jdbcTemplate.queryForObject(
				"SELECT VIEW_COUNT FROM NOTICE WHERE NOTICE_ID = 1",
				Integer.class
		);

		mockMvc.perform(get("/api/notices/{noticeId}", 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.noticeId").value(1))
				.andExpect(jsonPath("$.data.title").value("WithTrip 서비스 이용 안내"))
				.andExpect(jsonPath("$.data.viewCount").value(before + 1));

		Integer after = jdbcTemplate.queryForObject(
				"SELECT VIEW_COUNT FROM NOTICE WHERE NOTICE_ID = 1",
				Integer.class
		);
		assertThat(after).isEqualTo(before + 1);
	}

	@Test
	void hidesNonPublicAndMissingNotices() throws Exception {
		mockMvc.perform(get("/api/notices/{noticeId}", 4))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOTICE_NOT_FOUND"));

		mockMvc.perform(get("/api/notices/{noticeId}", 9999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOTICE_NOT_FOUND"));
	}
}
