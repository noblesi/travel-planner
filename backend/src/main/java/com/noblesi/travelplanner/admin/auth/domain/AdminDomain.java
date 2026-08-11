package com.noblesi.travelplanner.admin.auth.domain;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDomain {
	
	private long adminId;
	private String loginId;
	private String password;
	private String name;
	private String email;
	private String phoneNumber;
	private String adminRoleCode;
	private String adminStatus;
	private OffsetDateTime createdAt;
}
