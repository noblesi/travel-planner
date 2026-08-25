package com.noblesi.travelplanner.dto.member;

import java.time.LocalDate;

import com.noblesi.travelplanner.domain.member.MemberProfile;

public record MemberProfileResponse(
		String memberId,
		String name,
		String email,
		String nickname,
		LocalDate birthDate,
		String genderCode,
		String phoneNumber,
		String profileImageUrl
) {

	public static MemberProfileResponse from(MemberProfile profile) {
		return new MemberProfileResponse(
				Long.toString(profile.memberId()),
				profile.name(),
				profile.email(),
				profile.nickname(),
				profile.birthDate(),
				profile.genderCode(),
				profile.phoneNumber(),
				profile.profileImageUrl()
		);
	}
}
