package com.noblesi.travelplanner.admin.member.dto;

public record AdminMemberSearchDTO(String keyword, String memberStatus, int page, int size) {

	public AdminMemberSearchDTO {
		keyword = keyword == null ? "" : keyword.strip();
		memberStatus = memberStatus == null ? "" : memberStatus.strip().toUpperCase();
		page = Math.max(page, 1);
		size = Math.max(size, 1);
	}
}
