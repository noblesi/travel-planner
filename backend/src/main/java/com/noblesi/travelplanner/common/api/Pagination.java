package com.noblesi.travelplanner.common.api;

public record Pagination(
		int page,
		int size,
		long totalCount,
		int totalPages,
		int startPage,
		int endPage,
		int offset
) {
	private static final int PAGE_GROUP_SIZE = 10;

	public static Pagination of(int page, int size, long totalCount) {
		int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalCount / size);
		int startPage = ((page - 1) / PAGE_GROUP_SIZE) * PAGE_GROUP_SIZE + 1;
		int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, Math.max(totalPages, 1));
		int offset = (page - 1) * size;
		return new Pagination(page, size, totalCount, totalPages, startPage, endPage, offset);
	}

	public boolean hasPrevious() {
		return page > 1;
	}

	public boolean hasNext() {
		return page < totalPages;
	}
}
