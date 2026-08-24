package com.noblesi.travelplanner.admin.tour.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.admin.tour.dto.TourSyncHistoryDTO;
import com.noblesi.travelplanner.admin.tour.mapper.AdminTourSyncMapper;
import com.noblesi.travelplanner.admin.tour.AdminTourSyncTestSchema;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class AdminTourSyncHistoryIntegrationTest {

	@Autowired
	private AdminTourSyncMapper adminTourSyncMapper;

	@Autowired
	private AdminTourSyncService adminTourSyncService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void prepareTourSyncHistoryTable() {
		AdminTourSyncTestSchema.create(jdbcTemplate);
	}

	@Test
	void storesAndReadsRecentHistoryFromDatabase() {
		OffsetDateTime baseTime = OffsetDateTime.parse("2026-08-24T10:00:00+09:00");

		// 서로 다른 상태를 저장해 DB 코드가 관리자 화면 문구로 변환되는지도 함께 검증한다.
		insert(baseTime, 30, 0, "SUCCESS", null);
		insert(baseTime.plusMinutes(1), 20, 1, "PARTIAL_SUCCESS", "1개 지역 동기화 실패");
		insert(baseTime.plusMinutes(2), 0, 1, "FAILED", "인증키 오류");

		assertThat(adminTourSyncService.getHistory())
				.extracting(
						TourSyncHistoryDTO::changedCount,
						TourSyncHistoryDTO::failedCount,
						TourSyncHistoryDTO::status,
						TourSyncHistoryDTO::manager
				)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(0, 1, "실패", "admin1"),
						org.assertj.core.groups.Tuple.tuple(20, 1, "부분 성공", "admin1"),
						org.assertj.core.groups.Tuple.tuple(30, 0, "성공", "admin1")
				);
	}

	@Test
	void limitsHistoryToTenMostRecentRows() {
		OffsetDateTime baseTime = OffsetDateTime.parse("2026-08-24T11:00:00+09:00");
		for (int index = 0; index < 12; index++) {
			insert(baseTime.plusMinutes(index), index, 0, "SUCCESS", null);
		}

		assertThat(adminTourSyncService.getHistory())
				.hasSize(10)
				.first()
				.extracting(TourSyncHistoryDTO::changedCount)
				.isEqualTo(11);
	}

	private void insert(
			OffsetDateTime startedAt,
			int successCount,
			int failCount,
			String status,
			String errorMessage
	) {
		adminTourSyncMapper.insertHistory(
				"admin1",
				startedAt,
				startedAt.plusSeconds(10),
				successCount,
				failCount,
				status,
				errorMessage
		);
	}
}
