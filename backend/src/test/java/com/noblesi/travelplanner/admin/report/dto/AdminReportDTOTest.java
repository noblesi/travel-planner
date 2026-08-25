package com.noblesi.travelplanner.admin.report.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminReportDTOTest {

	@Test
	void convertsReportCodesToKoreanLabels() {
		AdminReportDTO report = new AdminReportDTO();
		report.setReasonCode("INAPPROPRIATE");
		report.setReportStatus("PENDING");
		report.setProcessResultCode("WARNING");

		assertThat(report.getReasonLabel()).isEqualTo("부적절한 콘텐츠");
		assertThat(report.getReportStatusLabel()).isEqualTo("검토 대기");
		assertThat(report.getProcessResultLabel()).isEqualTo("경고");
	}

	@Test
	void returnsSafeLabelsForMissingOrUnknownCodes() {
		AdminReportDTO report = new AdminReportDTO();
		assertThat(report.getReasonLabel()).isEmpty();
		assertThat(report.getReportStatusLabel()).isEmpty();
		assertThat(report.getProcessResultLabel()).isEmpty();

		report.setReasonCode("UNKNOWN");
		report.setReportStatus("UNKNOWN");
		report.setProcessResultCode("UNKNOWN");
		assertThat(report.getReasonLabel()).isEqualTo("알 수 없음");
		assertThat(report.getReportStatusLabel()).isEqualTo("알 수 없음");
		assertThat(report.getProcessResultLabel()).isEqualTo("알 수 없음");
	}
}
