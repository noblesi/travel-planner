package com.noblesi.travelplanner.admin.trip.dto;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminTripListDTO {

	private Long planId;
	private String title;
	private String regionName;
	private String ownerName;
	private Date startDate;
	private Date endDate;
	private int likeCount;
	private int viewCount;
	private String visibility;
	private String planStatus;
	private int reportCount;
	private Long latestReportId;
	private String latestReportStatus;

	public String getVisibilityLabel() {
		if (visibility == null) {
			return "";
		}
		return "PUBLIC".equalsIgnoreCase(visibility) ? "공개" : "비공개";
	}

	public String getReportStatusLabel() {
		if (latestReportStatus == null) {
			return "신고 처리 미완료";
		}
		return switch (latestReportStatus.toUpperCase()) {
			case "RESOLVED", "COMPLETED" -> "신고 처리 완료";
			default -> "신고 처리 미완료";
		};
	}
}
