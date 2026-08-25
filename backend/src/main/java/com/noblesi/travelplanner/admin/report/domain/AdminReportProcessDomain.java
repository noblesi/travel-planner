package com.noblesi.travelplanner.admin.report.domain;

import java.sql.Date;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AdminReportProcessDomain {

	private Long reportId;
	private Long adminId;
	private String processResultCode;
	private String processReason;
	private Date processedAt;
	
}//class
