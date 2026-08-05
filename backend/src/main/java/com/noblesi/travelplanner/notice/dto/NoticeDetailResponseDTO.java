package com.noblesi.travelplanner.notice.dto;

import java.time.LocalDate;

public record NoticeDetailResponseDTO(
		Long noticeId,
		String category,
		String title,
		String content,
		int viewCount,
		LocalDate createdAt
) {
}
