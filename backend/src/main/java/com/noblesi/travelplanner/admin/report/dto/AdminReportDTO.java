package com.noblesi.travelplanner.admin.report.dto;

import java.sql.Date;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Alias("adminReportDTO")
@Getter
@Setter
@ToString
public class AdminReportDTO {

	private Long reportId;
	private Long planId;
	private Long reporterMemberId;
	
	private String reporterName;
	private String planTitle;
	private String reasonCode;
	private String reasonDetail;
	private String reportStatus;
	
	private String processResultCode;
	private String processReason;
	
	private Date createdAt;
	private Date processedAt;
	
	
}//class 
