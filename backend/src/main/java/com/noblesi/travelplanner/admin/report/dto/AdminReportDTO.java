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

	public String getReasonLabel() {
		if (reasonCode == null) {
			return "";
		}
		return switch (reasonCode.toUpperCase()) {
			case "INAPPROPRIATE" -> "부적절한 콘텐츠";
			case "FALSE_INFO" -> "허위 정보";
			case "SPAM" -> "스팸/광고성";
			case "OTHER" -> "기타";
			default -> "알 수 없음";
		};
	}

	public String getReportStatusLabel() {
		if (reportStatus == null) {
			return "";
		}
		return switch (reportStatus.toUpperCase()) {
			case "PENDING", "RECEIVED" -> "검토 대기";
			case "IN_PROGRESS" -> "검토 중";
			case "RESOLVED", "COMPLETED" -> "검토 완료";
			case "REJECTED" -> "반려";
			default -> "알 수 없음";
		};
	}

	public String getProcessResultLabel() {
		if (processResultCode == null) {
			return "";
		}
		return switch (processResultCode.toUpperCase()) {
			case "REJECTED" -> "반려";
			case "WARNING" -> "경고";
			case "HIDDEN" -> "숨김";
			default -> "알 수 없음";
		};
	}

}//class 
