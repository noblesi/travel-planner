package com.noblesi.travelplanner.admin.member.domain;

import java.sql.Date;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AdminMemberDomain {

	private int memberId;
	private String memberName;
	private String email;
	private String nickName;
	private Date birthDate;
	private String phoneNumber;
	private String memberStatus;
	private Date createdAt;
}//class
