package com.noblesi.travelplanner.plansearch.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.plansearch.dto.ReportRequestDTO;

@Mapper
public interface ReportDAO {

	// 플랜 신고 저장
	int insertReport(
			@Param("memberId") Long memberId,
			@Param("planId") Long planId,
			@Param("request") ReportRequestDTO request
	);

	// 동일 회원의 동일 플랜 중복 신고를 API에서 명확한 Conflict로 응답하기 위해 사전 확인한다.
	boolean existsReport(@Param("memberId") Long memberId, @Param("planId") Long planId);
}
