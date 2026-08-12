package com.noblesi.travelplanner.admin.notice.dto;

public record AdminNoticeSearchDTO(String keyword, String categoryCode, int page, int size) {
	public AdminNoticeSearchDTO {
		keyword = keyword == null ? "" : keyword.strip();
		categoryCode = categoryCode == null ? "" : categoryCode.strip().toUpperCase();
		page = Math.max(page, 1);
		size = Math.max(size, 1);
	}
}
