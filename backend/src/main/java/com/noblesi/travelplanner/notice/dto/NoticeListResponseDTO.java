package com.noblesi.travelplanner.notice.dto;

import java.time.LocalDate;

public record NoticeListResponseDTO(
		Long noticeId,
		String category,
		String title,
		LocalDate createdAt
) {
}
