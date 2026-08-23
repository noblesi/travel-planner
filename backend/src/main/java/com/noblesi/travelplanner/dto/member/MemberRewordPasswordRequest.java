package com.noblesi.travelplanner.dto.member;

public record MemberRewordPasswordRequest(
    String currentPassword,
    String rewordPassword
) {
    
}
