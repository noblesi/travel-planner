package com.noblesi.travelplanner.admin.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.report.domain.AdminReportProcessDomain;
import com.noblesi.travelplanner.admin.report.dto.AdminReportDTO;

@Mapper
public interface AdminReportMapper {
	
	/**
	 * 신고 페이지 전체 정보 조회
	 * @param reportId
	 * @return
	 */
	AdminReportDTO selectReportDetail(@Param("reportId") Long reportId );
	
	int updateReportStatus(@Param("reportId") Long reportId, @Param("reportStatus") String reportStatus);
	
	int insertReportProcess(AdminReportProcessDomain arpDomain);
	
	int updatePlanVisibility(@Param("planId") Long planId, @Param("visibility") String visibility );
	
	
}//interface
