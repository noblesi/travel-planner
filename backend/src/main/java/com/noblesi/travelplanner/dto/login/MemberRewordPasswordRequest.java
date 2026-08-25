package com.noblesi.travelplanner.dto.login;

import org.apache.ibatis.type.Alias;

@Alias("memberRewordPass")
public record MemberRewordPasswordRequest(
    String email,
    String newPassword
) {
    
}
