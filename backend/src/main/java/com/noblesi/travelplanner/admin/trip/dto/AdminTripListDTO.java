package com.noblesi.travelplanner.admin.trip.dto;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminTripListDTO {

	private int planId;
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
	private Integer latestReportId;

	public String getVisibilityLabel() {
		if (visibility == null) {
			return "";
		}
		return "PUBLIC".equalsIgnoreCase(visibility) ? "공개" : "비공개";
	}
}
