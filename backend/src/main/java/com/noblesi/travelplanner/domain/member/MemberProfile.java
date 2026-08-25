package com.noblesi.travelplanner.domain.member;

import java.time.LocalDate;

public record MemberProfile(
		long memberId,
		String name,
		String email,
		String nickname,
		LocalDate birthDate,
		String genderCode,
		String phoneNumber,
		String profileImageUrl
) {
}
