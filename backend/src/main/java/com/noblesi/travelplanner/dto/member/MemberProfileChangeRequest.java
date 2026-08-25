package com.noblesi.travelplanner.dto.member;

import org.apache.ibatis.type.Alias;

@Alias("memberProfileChangeRequest")
public record MemberProfileChangeRequest(
    long memberId,
    String profileImageUrl
) {
    
}
