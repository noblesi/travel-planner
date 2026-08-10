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
	public int insertReport(Long memberId, ReportRequestDTO request) {
		Map<String, Object> params = new HashMap<>();
		params.put("memberId", memberId);
		params.put("request", request);
		return sqlSession.insert(NAMESPACE + "insertReport", params);
	}
}
