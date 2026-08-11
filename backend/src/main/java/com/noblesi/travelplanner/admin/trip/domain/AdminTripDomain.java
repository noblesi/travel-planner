package com.noblesi.travelplanner.admin.trip.domain;

import java.sql.Date;

import lombok.Getter;

@Getter
public class AdminTripDomain {

	private int planId;
	private Integer sourcePlanId;
	private int ownerMemberId;
	private String title;
	private String regionCode;
	private Date startDate;
	private Date endDate;
	private String visibility;
	private String planStatus;
	private int versionNo;
	private Date deletedAt;
	private Integer deletedByMemberId;
	private Date restoredAt;
	private Integer restoredByAdminId;
	private String thumbnailImg;
	private Date createAt;
	private Date updateAt;
	private int viewCount;
}
