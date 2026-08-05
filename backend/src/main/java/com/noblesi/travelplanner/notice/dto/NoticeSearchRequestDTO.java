package com.noblesi.travelplanner.notice.dto;

public record NoticeSearchRequestDTO(String category, int page, int size) {

	private static final int DEFAULT_SIZE = 10;
	private static final int MAX_SIZE = 100;

	public NoticeSearchRequestDTO {
		page = page < 1 ? 1 : page;
		size = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
	}
}
