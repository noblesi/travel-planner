package com.noblesi.travelplanner.admin.trip.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminTripReportDTOTest {

	@Test
	void translatesEveryReportStatusToKorean() {
		assertThat(report("PENDING").getReportStatusLabel()).isEqualTo("검토 대기");
		assertThat(report("RECEIVED").getReportStatusLabel()).isEqualTo("검토 대기");
		assertThat(report("IN_PROGRESS").getReportStatusLabel()).isEqualTo("검토 중");
		assertThat(report("RESOLVED").getReportStatusLabel()).isEqualTo("검토 완료");
		assertThat(report("COMPLETED").getReportStatusLabel()).isEqualTo("검토 완료");
		assertThat(report("REJECTED").getReportStatusLabel()).isEqualTo("반려");
		assertThat(report("UNKNOWN").getReportStatusLabel()).isEqualTo("알 수 없음");
	}

	private AdminTripReportDTO report(String status) {
		return new AdminTripReportDTO(1L, 1L, "신고자", "OTHER", null, status, null);
	}
}
