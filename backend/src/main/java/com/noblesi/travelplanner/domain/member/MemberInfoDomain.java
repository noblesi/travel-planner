package com.noblesi.travelplanner.domain.member;

import java.util.Date;

public record MemberInfoDomain(
    long memberId, 
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
