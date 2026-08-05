package com.noblesi.travelplanner.admin.auth.dto;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDTO {

	private int adminId;
	private String loginId;
	private String password;
	private String name;
	private String email;
	private String phoneNumber;
	private String adminRoleCode;
	private String adminStatus;
	private Date createAt;
}
