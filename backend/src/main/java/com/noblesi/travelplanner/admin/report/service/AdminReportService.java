package com.noblesi.travelplanner.admin.report.service;

import java.sql.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.admin.report.domain.AdminReportProcessDomain;
import com.noblesi.travelplanner.admin.report.dto.AdminReportDTO;
import com.noblesi.travelplanner.admin.report.dto.AdminReportProcessDTO;
import com.noblesi.travelplanner.admin.report.mapper.AdminReportMapper;

@Service
public class AdminReportService {

	@Autowired
	private AdminReportMapper adminReportMapper;

	public AdminReportDTO getReportDetail(Long reportId) {
		AdminReportDTO report = adminReportMapper.selectReportDetail(reportId);
		if (report == null) {
			throw new IllegalArgumentException("신고 정보를 찾을 수 없습니다.");
		}
		return report;
	}

	@Transactional
	public void completeReport(AdminReportProcessDTO processDTO, Long adminId) {
		AdminReportDTO report = getReportDetail(processDTO.getReportId());

		AdminReportProcessDomain reportProcess = AdminReportProcessDomain.builder()
				.reportId(processDTO.getReportId())
				.adminId(adminId)
				.processResultCode(processDTO.getProcessResultCode())
				.processReason(processDTO.getProcessReason())
				.processedAt(new Date(System.currentTimeMillis()))
				.build();

		adminReportMapper.insertReportProcess(reportProcess);
		adminReportMapper.updateReportStatus(processDTO.getReportId(), "RESOLVED");

		if ("HIDDEN".equals(processDTO.getProcessResultCode())) {
			adminReportMapper.updatePlanVisibility(report.getPlanId(), "PRIVATE");
		}
	}
}
