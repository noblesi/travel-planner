package com.noblesi.travelplanner.notice.dto;

import java.time.LocalDate;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

@Alias("NoticeDetailResponseDTO")
@Getter
@Setter
public class NoticeDetailResponseDTO {

	private Long noticeId;
	private String category;
	private String title;
	private String content;
	private int viewCount;
	private LocalDate createdAt;
}
