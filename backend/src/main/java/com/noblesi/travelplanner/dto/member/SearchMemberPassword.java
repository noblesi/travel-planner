package com.noblesi.travelplanner.dto.member;

import org.apache.ibatis.type.Alias;

@Alias("searchMemberPassword")
public record SearchMemberPassword(
    long memberId,
    String currentPassword
) {
    
}
