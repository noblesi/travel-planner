package com.noblesi.travelplanner.admin.report.dto;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Alias("adminReportProcessDTO")
@Getter
@Setter
@ToString
public class AdminReportProcessDTO {

	private int reportId;
	private String processResultCode;
	private String processReason;
	
}//class
