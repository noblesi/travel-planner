package com.noblesi.travelplanner.admin.member.dto;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminMemberDetailDTO {

	private Long memberId;
	private String memberName;
	private String nickName;
	private String email;
	private String phoneNumber;
	private Date birthDate;
	private String memberStatus;
	private Date createdAt;
	private int planCount;
	private int reportedPlanCount;
}
