package com.noblesi.travelplanner.dto.member;

import org.apache.ibatis.type.Alias;

@Alias("memberRewordPassword")
public record MemberRewordPassword(
    long memberId,
    String rewordPassword
) {
    
}
