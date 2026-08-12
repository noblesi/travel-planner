package com.noblesi.travelplanner.dto.member;

import org.apache.ibatis.type.Alias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Alias("joinMemberRequest")
public record JoinMemberRequest(
    @NotBlank(message = "이메일이 올바르지 않습니다.") 
    @Email(message = "이메일이 올바르지 않습니다.") 
    String email,
    
    @NotBlank(message = "비밀번호가 올바르지 않습니다.") 
    String password,
    
    @NotBlank(message = "이름이 올바르지 않습니다.") 
    String name,
    String nickname,
    String gender,
    String birth,
    String privacy,
    
    @NotBlank(message = "전화번호가 올바르지 않습니다.")
    String phone
) {
    public JoinMemberRequest {
        // nickname 값이 비어있거나 null이면 name 값으로 채워넣습니다.
        if (nickname == null || nickname.isBlank()) {
            nickname = name;
        }
    }
}
