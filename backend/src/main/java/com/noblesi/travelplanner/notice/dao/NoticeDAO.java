package com.noblesi.travelplanner.notice.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.noblesi.travelplanner.notice.dto.NoticeDetailResponseDTO;
import com.noblesi.travelplanner.notice.dto.NoticeListResponseDTO;
import com.noblesi.travelplanner.notice.dto.NoticeSearchRequestDTO;

@Repository
public class NoticeDAO {

	private static final String NAMESPACE = "com.noblesi.travelplanner.notice.dao.NoticeDAO.";

	private final SqlSession sqlSession;

	public NoticeDAO(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	// 공지사항 목록 조회 (카테고리 필터 + 페이징)
	public List<NoticeListResponseDTO> selectNoticeList(NoticeSearchRequestDTO request) {
		return sqlSession.selectList(NAMESPACE + "selectNoticeList", request);
	}

	// 카테고리 조건에 맞는 공지사항 전체 개수 조회 (페이징 계산용)
	public long countNoticeList(String category) {
		Map<String, Object> params = new HashMap<>();
		params.put("category", category);
		Long count = sqlSession.selectOne(NAMESPACE + "countNoticeList", params);
		return count == null ? 0L : count;
	}

	// 공지사항 상세 조회
	public NoticeDetailResponseDTO selectNoticeById(Long noticeId) {
		Map<String, Object> params = new HashMap<>();
		params.put("noticeId", noticeId);
		return sqlSession.selectOne(NAMESPACE + "selectNoticeById", params);
	}

	// 공지사항 조회수 +1
	public int updateNoticeViewCount(Long noticeId) {
		Map<String, Object> params = new HashMap<>();
		params.put("noticeId", noticeId);
		return sqlSession.update(NAMESPACE + "updateNoticeViewCount", params);
	}
}
