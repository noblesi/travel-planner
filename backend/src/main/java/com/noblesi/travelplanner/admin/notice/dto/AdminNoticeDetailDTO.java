package com.noblesi.travelplanner.admin.notice.dto;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminNoticeDetailDTO {
	private Long noticeId;
	private Long adminId;
	private String adminName;
	private String title;
	private String content;
	private String categoryCode;
	private int viewCount;
	private OffsetDateTime createdAt;
}
