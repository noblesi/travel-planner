package com.noblesi.travelplanner.admin.auth.dto;

import java.sql.Date;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDTO {

	private int adminId;

	@NotBlank(message = "관리자 아이디를 입력해 주세요.")
	private String loginId;

	@NotBlank(message = "비밀번호를 입력해 주세요.")
	private String password;

	private String name;
	private String email;
	private String phoneNumber;
	private String adminRoleCode;
	private String adminStatus;
	private Date createAt;
}
