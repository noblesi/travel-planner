package com.noblesi.travelplanner.admin.trip.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminTripReportDTOTest {

	@Test
	void translatesEveryReportReasonToKorean() {
		assertThat(report("PENDING", "INAPPROPRIATE").getReasonLabel()).isEqualTo("부적절한 콘텐츠");
		assertThat(report("PENDING", "FALSE_INFO").getReasonLabel()).isEqualTo("허위 정보");
		assertThat(report("PENDING", "SPAM").getReasonLabel()).isEqualTo("스팸/광고성");
		assertThat(report("PENDING", "OTHER").getReasonLabel()).isEqualTo("기타");
		assertThat(report("PENDING", "UNKNOWN").getReasonLabel()).isEqualTo("알 수 없음");
	}

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
		return report(status, "OTHER");
	}

	private AdminTripReportDTO report(String status, String reasonCode) {
		return new AdminTripReportDTO(1L, 1L, "신고자", reasonCode, null, status, null);
	}
}
