package com.noblesi.travelplanner.notice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoticeSearchRequestDTO {

	private static final int DEFAULT_SIZE = 10;
	private static final int MAX_SIZE = 100;

	private String category;
	private int page;
	private int size;

	public NoticeSearchRequestDTO(String category, int page, int size) {
		this.category = category;
		this.page = page < 1 ? 1 : page;
		this.size = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
	}
}
