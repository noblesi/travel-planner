package com.noblesi.travelplanner.dto.member;

import java.time.LocalDate;

import org.apache.ibatis.type.Alias;

import com.fasterxml.jackson.annotation.JsonFormat;

@Alias("memberInfoRequest")
public record MemberInfoRequest(
    Long memberId, 
    String memberName,
    String email,
    String genderCode,
    String phoneNumber,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate birthDate
) {
    
}
