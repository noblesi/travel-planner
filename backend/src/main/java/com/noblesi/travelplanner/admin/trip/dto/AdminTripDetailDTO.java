package com.noblesi.travelplanner.admin.trip.dto;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminTripDetailDTO {

	private int planId;
	private String title;
	private String regionName;
	private int ownerMemberId;
	private String ownerName;
	private Date startDate;
	private Date endDate;
	private String visibility;
	private String planStatus;
	private int participantCount;
	private int likeCount;
	private int viewCount;
	private int copyCount;

	public String getVisibilityLabel() {
		if (visibility == null) {
			return "";
		}
		return switch (visibility.toUpperCase()) {
			case "PUBLIC" -> "공개";
			case "PRIVATE" -> "비공개";
			default -> visibility;
		};
	}

	public String getPlanStatusLabel() {
		if (planStatus == null) {
			return "";
		}
		return switch (planStatus.toUpperCase()) {
			case "ACTIVE" -> "운영 중";
			case "DELETED" -> "삭제됨";
			default -> planStatus;
		};
	}
}
