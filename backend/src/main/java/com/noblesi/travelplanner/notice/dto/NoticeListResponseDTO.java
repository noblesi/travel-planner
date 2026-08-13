package com.noblesi.travelplanner.notice.dto;

import java.time.LocalDate;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

@Alias("NoticeListResponseDTO")
@Getter
@Setter
public class NoticeListResponseDTO {

	private Long noticeId;
	private String category;
	private String title;
	private LocalDate createdAt;
}
