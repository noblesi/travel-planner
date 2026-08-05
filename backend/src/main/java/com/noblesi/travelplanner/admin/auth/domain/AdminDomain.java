package com.noblesi.travelplanner.admin.auth.domain;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AdminDomain {
	
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
