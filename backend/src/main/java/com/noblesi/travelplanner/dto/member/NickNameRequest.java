package com.noblesi.travelplanner.dto.member;

import org.apache.ibatis.type.Alias;

@Alias("nickNameRequest")
public record NickNameRequest(
    Long memberId, 
    String nickname
) {
    
}