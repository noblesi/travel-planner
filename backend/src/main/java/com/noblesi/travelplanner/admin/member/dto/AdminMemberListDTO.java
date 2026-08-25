package com.noblesi.travelplanner.admin.member.dto;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminMemberListDTO {

	private Long memberId;
	private String memberName;
	private String memberNickName;
	private String email;
	private Date createdAt;
	private String memberStatus;
	private int planCount;
	private int reportedPlanCount;
	
}//AdminMemberListDTO
