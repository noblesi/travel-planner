package com.noblesi.travelplanner.dto.login;

import java.util.Locale;

import org.apache.ibatis.type.Alias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Alias("emailLoginRequest")
public record EmailLoginRequest(
		@NotBlank(message = "이메일을 입력해 주세요.")
		@Email(message = "올바른 이메일 형식이 아닙니다.")
		String email,

		@NotBlank(message = "비밀번호를 입력해 주세요.")
		String password
) {
	public EmailLoginRequest {
		if (email != null) {
			email = email.trim().toLowerCase(Locale.ROOT);
		}
	}
	
}
