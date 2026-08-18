package com.noblesi.travelplanner.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDTO {

	@NotBlank(message = "관리자 아이디를 입력해 주세요.")
	private String loginId;

	@NotBlank(message = "비밀번호를 입력해 주세요.")
	private String password;

}
