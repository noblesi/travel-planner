package com.noblesi.travelplanner.plansearch.dao;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

@Repository
public class PlanLikeDAO {

	private static final String NAMESPACE = "com.noblesi.travelplanner.plansearch.dao.PlanLikeDAO.";
	private final SqlSession sqlSession;

	public PlanLikeDAO(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	// 좋아요 등록
	public int insertLike(Long memberId, Long planId) {
		return sqlSession.insert(NAMESPACE + "insertLike", params(memberId, planId));
	}

	// 좋아요 삭제
	public int deleteLike(Long memberId, Long planId) {
		return sqlSession.delete(NAMESPACE + "deleteLike", params(memberId, planId));
	}

	// 좋아요 여부 확인
	public boolean existsLike(Long memberId, Long planId) {
		Boolean exists = sqlSession.selectOne(NAMESPACE + "existsLike", params(memberId, planId));
		return exists != null && exists;
	}

	private Map<String, Object> params(Long memberId, Long planId) {
		Map<String, Object> params = new HashMap<>();
		params.put("memberId", memberId);
		params.put("planId", planId);
		return params;
	}
}
