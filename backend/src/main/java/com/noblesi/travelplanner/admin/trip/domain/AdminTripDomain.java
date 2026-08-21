package com.noblesi.travelplanner.admin.trip.domain;

import java.sql.Date;

import lombok.Getter;

@Getter
public class AdminTripDomain {

	private Long planId;
	private Long sourcePlanId;
	private Long ownerMemberId;
	private String title;
	private String regionCode;
	private Date startDate;
	private Date endDate;
	private String visibility;
	private String planStatus;
	private int versionNo;
	private Date deletedAt;
	private Long deletedByMemberId;
	private Date restoredAt;
	private Long restoredByAdminId;
	private String thumbnailImg;
	private Date createdAt;
	private Date updatedAt;
	private int viewCount;
}
