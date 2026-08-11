package com.noblesi.travelplanner.plansearch.dao;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.noblesi.travelplanner.plansearch.dto.PlanDetailDayDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanListResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanSearchRequestDTO;

@Repository
public class PlanSearchDAO {

	private static final String NAMESPACE = "com.noblesi.travelplanner.plansearch.dao.PlanSearchDAO.";
	private final SqlSession sqlSession;

	public PlanSearchDAO(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	// 공개 플랜 개수 조회
	public long countPlanList(PlanSearchRequestDTO request) {
		Long count = sqlSession.selectOne(NAMESPACE + "countPlanList", request);
		return count == null ? 0L : count;
	}

	// 공개 플랜 목록 조회
	public List<PlanListResponseDTO> selectPlanList(PlanSearchRequestDTO request) {
		return sqlSession.selectList(NAMESPACE + "selectPlanList", request);
	}

	// 플랜 상세 조회 (일차/장소 제외)
	public PlanDetailResponseDTO selectPlanById(Long planId) {
		return sqlSession.selectOne(NAMESPACE + "selectPlanById", planId);
	}

	// 플랜 조회 수 증가
	public int updatePlanViewCount(Long planId) {
		return sqlSession.update(NAMESPACE + "updatePlanViewCount", planId);
	}

	// 여행 일차 목록 조회
	public List<PlanDetailDayDTO> selectPlanDays(Long planId) {
		return sqlSession.selectList(NAMESPACE + "selectPlanDays", planId);
	}

	// 여행 일정 항목 평면 조회
	public List<PlanScheduleRowDTO> selectPlanSchedule(Long planId) {
		return sqlSession.selectList(NAMESPACE + "selectPlanSchedule", planId);
	}

	// 모든 사용자 액션이 같은 공개 완료 조건을 사용하도록 대상 플랜의 최소 정보만 한 번에 조회한다.
	public PublishedPlanTargetDTO selectPublishedPlanTarget(Long planId) {
		return sqlSession.selectOne(NAMESPACE + "selectPublishedPlanTarget", planId);
	}

	// 새 여행 플랜 ID 채번
	public long nextTravelPlanId() {
		return sqlSession.selectOne(NAMESPACE + "nextTravelPlanId");
	}

	// 새 여행 플랜 저장 (복사본, 비공개로 생성)
	public int insertTravelPlan(
			Long planId,
			Long sourcePlanId,
			Long memberId,
			String title,
			String regionCode,
			LocalDate startDate,
			LocalDate endDate
	) {
		Map<String, Object> params = new HashMap<>();
		params.put("planId", planId);
		params.put("sourcePlanId", sourcePlanId);
		params.put("memberId", memberId);
		params.put("title", title);
		params.put("regionCode", regionCode);
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		return sqlSession.insert(NAMESPACE + "insertTravelPlan", params);
	}

	// 플랜 참여자(생성자) 등록
	public int insertPlanMember(Long planId, Long memberId) {
		Map<String, Object> params = new HashMap<>();
		params.put("planId", planId);
		params.put("memberId", memberId);
		return sqlSession.insert(NAMESPACE + "insertPlanMember", params);
	}

	// 새 여행 일차 ID 채번
	public long nextPlanDayId() {
		return sqlSession.selectOne(NAMESPACE + "nextPlanDayId");
	}

	// 새 여행 일차 저장
	public int insertPlanDay(Long planDayId, Long planId, int dayNo, LocalDate travelDate) {
		Map<String, Object> params = new HashMap<>();
		params.put("planDayId", planDayId);
		params.put("planId", planId);
		params.put("dayNo", dayNo);
		params.put("travelDate", travelDate);
		return sqlSession.insert(NAMESPACE + "insertPlanDay", params);
	}

	// 원본 플랜의 일정 항목 전체 컬럼 조회 (복사용)
	public List<PlanScheduleCopyRowDTO> selectPlanScheduleForCopy(Long planId) {
		return sqlSession.selectList(NAMESPACE + "selectPlanScheduleForCopy", planId);
	}

	// 새 일정 항목 ID 채번
	public long nextScheduleItemId() {
		return sqlSession.selectOne(NAMESPACE + "nextScheduleItemId");
	}

	// 새 일정 항목 저장
	public int insertScheduleItem(Long scheduleItemId, Long planDayId, PlanScheduleCopyRowDTO source) {
		Map<String, Object> params = new HashMap<>();
		params.put("scheduleItemId", scheduleItemId);
		params.put("planDayId", planDayId);
		params.put("source", source);
		return sqlSession.insert(NAMESPACE + "insertScheduleItem", params);
	}
}
