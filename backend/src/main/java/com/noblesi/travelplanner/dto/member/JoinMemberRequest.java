package com.noblesi.travelplanner.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record JoinMemberRequest(
    @NotBlank(message = "이메일이 올바르지 않습니다.") 
    @Email(message = "이메일이 올바르지 않습니다.") 
    String email,
    
    @NotBlank(message = "비밀번호가 올바르지 않습니다.") 
    String password,
    
    @NotBlank(message = "이름이 올바르지 않습니다.") 
    String name,
    
    String gender,
    String birth,
    
    @NotBlank(message = "전화번호가 올바르지 않습니다.")
    String phone
) {
    
}
