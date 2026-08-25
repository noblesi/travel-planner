package com.noblesi.travelplanner.plansearch.dao;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.plansearch.dto.NewTravelPlanDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailDayDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanListResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanScheduleCopyRowDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanScheduleRowDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanSearchRequestDTO;
import com.noblesi.travelplanner.plansearch.dto.PublishedPlanTargetDTO;

@Mapper
public interface PlanSearchDAO {

	// 공개 플랜 개수 조회
	long countPlanList(PlanSearchRequestDTO request);

	// 공개 플랜 목록 조회
	List<PlanListResponseDTO> selectPlanList(PlanSearchRequestDTO request);

	// 플랜 상세 조회 (일차/장소 제외)
	PlanDetailResponseDTO selectPlanById(@Param("planId") Long planId);

	// 여행 일차 목록 조회
	List<PlanDetailDayDTO> selectPlanDays(@Param("planId") Long planId);

	// 여행 일정 항목 평면 조회
	List<PlanScheduleRowDTO> selectPlanSchedule(@Param("planId") Long planId);

	// 모든 사용자 액션이 같은 공개 완료 조건을 사용하도록 대상 플랜의 최소 정보만 한 번에 조회한다.
	PublishedPlanTargetDTO selectPublishedPlanTarget(@Param("planId") Long planId);

	// 새 여행 플랜 ID 채번
	long nextTravelPlanId();

	// 새 여행 플랜 저장 (복사본, 비공개로 생성)
	int insertTravelPlan(NewTravelPlanDTO newPlan);

	// 플랜 참여자(생성자) 등록
	int insertPlanMember(@Param("planId") Long planId, @Param("memberId") Long memberId);

	// 새 여행 일차 ID 채번
	long nextPlanDayId();

	// 새 여행 일차 저장
	int insertPlanDay(
			@Param("planDayId") Long planDayId,
			@Param("planId") Long planId,
			@Param("dayNo") int dayNo,
			@Param("travelDate") LocalDate travelDate
	);

	// 원본 플랜의 일정 항목 전체 컬럼 조회 (복사용)
	List<PlanScheduleCopyRowDTO> selectPlanScheduleForCopy(@Param("planId") Long planId);

	// 새 일정 항목 ID 채번
	long nextScheduleItemId();

	// 새 일정 항목 저장
	int insertScheduleItem(
			@Param("scheduleItemId") Long scheduleItemId,
			@Param("planDayId") Long planDayId,
			@Param("source") PlanScheduleCopyRowDTO source
	);
}
