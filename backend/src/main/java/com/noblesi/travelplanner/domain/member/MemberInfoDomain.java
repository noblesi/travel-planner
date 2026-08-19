package com.noblesi.travelplanner.domain.member;

import java.sql.Date;

public record MemberInfoDomain(
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
