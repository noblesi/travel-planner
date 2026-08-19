package com.noblesi.travelplanner.dto.member;

import java.sql.Date;

import org.apache.ibatis.type.Alias;

@Alias("memberInfoRequest")
public record MemberInfoRequest(
    Long memberId, 
    String memberName,
    String email, 
    String nickname,
    String genderCode,
    String phoneNumber,
    String profileImageUrl,
    String memberStatus,
    String createdAt,
    String withdrawnAt,
    Date birthDate
) {
    
}
