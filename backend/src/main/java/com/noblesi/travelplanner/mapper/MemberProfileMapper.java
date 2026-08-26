package com.noblesi.travelplanner.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.member.MemberProfile;
import com.noblesi.travelplanner.dto.member.UpdateMemberProfileRequest;

@Mapper
public interface MemberProfileMapper {

	Optional<MemberProfile> findActiveProfileByMemberId(@Param("memberId") long memberId);

	Optional<MemberProfile> findActiveProfileByMemberIdForUpdate(@Param("memberId") long memberId);

	int updateActiveProfile(
			@Param("memberId") long memberId,
			@Param("request") UpdateMemberProfileRequest request
	);

	Optional<String> findActivePasswordHashByMemberId(@Param("memberId") long memberId);

	int updateActivePassword(
			@Param("memberId") long memberId,
			@Param("passwordHash") String passwordHash
	);

	int updateActiveProfileImage(
			@Param("memberId") long memberId,
			@Param("profileImageUrl") String profileImageUrl
	);

	int withdrawActiveMember(@Param("memberId") long memberId);
}
