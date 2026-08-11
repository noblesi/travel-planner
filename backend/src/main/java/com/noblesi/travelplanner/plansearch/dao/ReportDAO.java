package com.noblesi.travelplanner.plansearch.dao;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.noblesi.travelplanner.plansearch.dto.ReportRequestDTO;

@Repository
public class ReportDAO {

	private static final String NAMESPACE = "com.noblesi.travelplanner.plansearch.dao.ReportDAO.";
	private final SqlSession sqlSession;

	public ReportDAO(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	// 플랜 신고 저장
	public int insertReport(Long memberId, Long planId, ReportRequestDTO request) {
		Map<String, Object> params = new HashMap<>();
		params.put("memberId", memberId);
		params.put("planId", planId);
		params.put("request", request);
		return sqlSession.insert(NAMESPACE + "insertReport", params);
	}

	// 동일 회원의 동일 플랜 중복 신고를 API에서 명확한 Conflict로 응답하기 위해 사전 확인한다.
	public boolean existsReport(Long memberId, Long planId) {
		Map<String, Object> params = new HashMap<>();
		params.put("memberId", memberId);
		params.put("planId", planId);
		Boolean exists = sqlSession.selectOne(NAMESPACE + "existsReport", params);
		return exists != null && exists;
	}
}
