package com.noblesi.travelplanner.plansearch.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.api.PageResponse;
import com.noblesi.travelplanner.common.api.Pagination;
import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.plansearch.dao.PlanLikeDAO;
import com.noblesi.travelplanner.plansearch.dao.PlanScheduleCopyRowDTO;
import com.noblesi.travelplanner.plansearch.dao.PlanScheduleRowDTO;
import com.noblesi.travelplanner.plansearch.dao.PlanSearchDAO;
import com.noblesi.travelplanner.plansearch.dao.ReportDAO;
import com.noblesi.travelplanner.plansearch.dto.PlanCopyRequestDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailDayDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailPlaceDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanListResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanSearchRequestDTO;
import com.noblesi.travelplanner.plansearch.dto.ReportRequestDTO;

@Service
public class PlanSearchService {

	private static final int MAX_SIZE = 100;

	private final PlanSearchDAO psd;
	private final PlanLikeDAO pld;
	private final ReportDAO rd;

	public PlanSearchService(PlanSearchDAO psd, PlanLikeDAO pld, ReportDAO rd) {
		this.psd = psd;
		this.pld = pld;
		this.rd = rd;
	}

	// 공개 플랜 목록 조회 (검색어 + 페이징)
	@Transactional(readOnly = true)
	public PageResponse<PlanListResponseDTO> searchPlanList(PlanSearchRequestDTO request) {
		int page = Math.max(request.getPage(), 1);
		int size = Math.min(Math.max(request.getSize(), 1), MAX_SIZE);
		request.setPage(page);
		request.setSize(size);

		long totalCount = psd.countPlanList(request);
		Pagination pagination = Pagination.of(page, size, totalCount);
		if (totalCount == 0) {
			return PageResponse.empty(pagination);
		}
		return PageResponse.of(psd.selectPlanList(request), pagination);
	}

	// 플랜 상세 조회 (memberId가 없으면 비로그인 조회로 보고 liked는 false로 처리)
	@Transactional(readOnly = true)
	public PlanDetailResponseDTO searchPlanDetail(Long planId, Long memberId) {
		PlanDetailResponseDTO detail = psd.selectPlanById(planId);
		if (detail == null) {
			throw planNotFound();
		}
		detail.setLiked(memberId != null && pld.existsLike(memberId, planId));
		detail.setDays(loadDays(planId));
		return detail;
	}

	// 일차 목록에 장소 목록을 채워서 조립
	private List<PlanDetailDayDTO> loadDays(Long planId) {
		List<PlanDetailDayDTO> days = psd.selectPlanDays(planId);
		Map<Integer, List<PlanDetailPlaceDTO>> placesByDay = new HashMap<>();
		for (PlanScheduleRowDTO row : psd.selectPlanSchedule(planId)) {
			PlanDetailPlaceDTO place = new PlanDetailPlaceDTO();
			place.setTimeSlot(row.getTimeSlot());
			place.setPlaceName(row.getPlaceName());
			place.setDescription(row.getDescription());
			place.setLatitude(row.getLatitude());
			place.setLongitude(row.getLongitude());
			placesByDay.computeIfAbsent(row.getDayNumber(), ignored -> new ArrayList<>()).add(place);
		}
		for (PlanDetailDayDTO day : days) {
			day.setPlaces(placesByDay.getOrDefault(day.getDayNumber(), new ArrayList<>()));
		}
		return days;
	}

	// 플랜 조회 수 증가
	@Transactional
	public void increasePlanViewCount(Long planId) {
		if (psd.updatePlanViewCount(planId) != 1) {
			throw planNotFound();
		}
	}

	// 좋아요 토글: 이미 눌렀으면 취소, 안 눌렀으면 등록하고 결과 상태를 반환
	@Transactional
	public boolean toggleLike(Long memberId, Long planId) {
		if (pld.existsLike(memberId, planId)) {
			pld.deleteLike(memberId, planId);
			return false;
		}
		pld.insertLike(memberId, planId);
		return true;
	}

	// 플랜 신고 저장
	public void reportPlan(Long memberId, ReportRequestDTO request) {
		rd.insertReport(memberId, request);
	}

	// 공개 플랜을 내 플랜으로 복사 (새 기간보다 긴 날짜의 일정은 버림)
	@Transactional
	public Long copyPlan(Long memberId, Long sourcePlanId, PlanCopyRequestDTO request) {
		if (request.getStartDate() == null || request.getEndDate() == null
				|| request.getStartDate().isAfter(request.getEndDate())) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "여행 시작일은 종료일보다 늦을 수 없습니다."
			);
		}

		String regionCode = psd.selectPlanRegionCode(sourcePlanId);
		if (regionCode == null) {
			throw planNotFound();
		}

		long newPlanId = psd.nextTravelPlanId();
		psd.insertTravelPlan(
				newPlanId, sourcePlanId, memberId, request.getTitle(),
				regionCode, request.getStartDate(), request.getEndDate()
		);
		psd.insertPlanMember(newPlanId, memberId);

		Map<Integer, Long> newDayIdByDayNo = new HashMap<>();
		LocalDate travelDate = request.getStartDate();
		int dayNo = 1;
		while (!travelDate.isAfter(request.getEndDate())) {
			long planDayId = psd.nextPlanDayId();
			psd.insertPlanDay(planDayId, newPlanId, dayNo, travelDate);
			newDayIdByDayNo.put(dayNo, planDayId);
			travelDate = travelDate.plusDays(1);
			dayNo++;
		}

		for (PlanScheduleCopyRowDTO row : psd.selectPlanScheduleForCopy(sourcePlanId)) {
			Long newPlanDayId = newDayIdByDayNo.get(row.getDayNumber());
			if (newPlanDayId == null) {
				continue;
			}
			psd.insertScheduleItem(psd.nextScheduleItemId(), newPlanDayId, row);
		}

		return newPlanId;
	}

	private BusinessException planNotFound() {
		return new BusinessException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "공개 여행 플랜을 찾을 수 없습니다.");
	}
}
