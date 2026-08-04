package com.noblesi.travelplanner.common.api;

import java.util.List;

public record PageResponse<T>(List<T> content, Pagination pagination) {

	public PageResponse {
		content = List.copyOf(content);
	}

	public static <T> PageResponse<T> of(List<T> content, Pagination pagination) {
		return new PageResponse<>(content, pagination);
	}

	public static <T> PageResponse<T> empty(Pagination pagination) {
		return new PageResponse<>(List.of(), pagination);
	}
}
