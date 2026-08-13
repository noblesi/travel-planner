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

	private int reportId;
	private int planId;
	private int reporterMemberId;
	
	private String reporterName;
	private String planTitle;
	private String reasonCode;
	private String reasonDetail;
	private String reportStatus;
	
	private String processResultCode;
	private String processReason;
	
	private Date createAt;
	private Date processedAt;
	
	
}//class 

