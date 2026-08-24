package com.noblesi.travelplanner.admin.trip.dto;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminTripReportDTO {

	private int reportId;
	private int reporterMemberId;
	private String reporterName;
	private String reasonCode;
	private String reasonDetail;
	private String reportStatus;
	private Date createdAt;

	public String getReportStatusLabel() {
		if (reportStatus == null) {
			return "";
		}
		return switch (reportStatus.toUpperCase()) {
			case "RESOLVED", "COMPLETED" -> "검토 완료";
			case "PENDING", "RECEIVED" -> "검토 대기";
			case "IN_PROGRESS" -> "검토 중";
			case "REJECTED" -> "반려";
			default -> "알 수 없음";
		};
	}
}
