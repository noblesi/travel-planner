package com.noblesi.travelplanner.admin.notice.dto;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminNoticeListDTO {
	private Long noticeId;
	private String title;
	private String categoryCode;
	private int viewCount;
	private String adminName;
	private OffsetDateTime createdAt;
}
