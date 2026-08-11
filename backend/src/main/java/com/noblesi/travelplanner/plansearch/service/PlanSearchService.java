package com.noblesi.travelplanner.plansearch.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
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
import com.noblesi.travelplanner.plansearch.dao.PublishedPlanTargetDTO;
import com.noblesi.travelplanner.plansearch.dao.ReportDAO;
import com.noblesi.travelplanner.plansearch.dto.PlanCopyRequestDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailDayDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailPlaceDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanListResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanSearchRequestDTO;
import com.noblesi.travelplanner.plansearch.dto.ReportRequestDTO;
import com.noblesi.travelplanner.service.TravelPlanRequestValidator;

@Service
public class PlanSearchService {

	private static final int MAX_SIZE = 100;

	private final PlanSearchDAO psd;
	private final PlanLikeDAO pld;
	private final ReportDAO rd;
	private final TravelPlanRequestValidator requestValidator;

	public PlanSearchService(
			PlanSearchDAO psd,
			PlanLikeDAO pld,
			ReportDAO rd,
			TravelPlanRequestValidator requestValidator
	) {
		this.psd = psd;
		this.pld = pld;
		this.rd = rd;
		this.requestValidator = requestValidator;
	}

	// 공개 플랜 목록 조회 (검색어 + 페이징)
	@Transactional(readOnly = true)
	public PageResponse<PlanListResponseDTO> searchPlanList(PlanSearchRequestDTO request) {
		int page = Math.max(request.getPage(), 1);
		int size = Math.min(Math.max(request.getSize(), 1), MAX_SIZE);
		String keyword = request.getKeyword() == null ? "" : request.getKeyword().trim();
		request.setKeyword(keyword);
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
		// 화면 노출 조건과 사용자 액션 조건을 통일해 비공개·DRAFT·삭제 플랜에 좋아요가 남지 않게 한다.
		requirePublishedPlan(planId);
		if (pld.existsLike(memberId, planId)) {
			pld.deleteLike(memberId, planId);
			return false;
		}
		try {
			pld.insertLike(memberId, planId);
		} catch (DuplicateKeyException exception) {
			// 동시 요청이 모두 미등록 상태를 읽어도 unique constraint 충돌을 500으로 노출하지 않고 최종 liked 상태로 수렴시킨다.
			return true;
		}
		return true;
	}

	// 플랜 신고 저장
	@Transactional
	public void reportPlan(Long memberId, Long planId, ReportRequestDTO request) {
		PublishedPlanTargetDTO target = requirePublishedPlan(planId);
		if (memberId.equals(target.getOwnerMemberId())) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"SELF_PLAN_REPORT_NOT_ALLOWED",
					"본인의 여행 플랜은 신고할 수 없습니다."
			);
		}
		if (rd.existsReport(memberId, planId)) {
			throw reportAlreadyExists();
		}

		if (request.getDetail() != null) {
			// 공백만 있는 선택 입력은 의미 있는 신고 내용이 아니므로 null로 정규화해 운영 데이터 품질을 유지한다.
			String normalizedDetail = request.getDetail().trim();
			request.setDetail(normalizedDetail.isEmpty() ? null : normalizedDetail);
		}
		try {
			rd.insertReport(memberId, planId, request);
		} catch (DuplicateKeyException exception) {
			// 사전 확인 이후 발생한 동시 중복 신고도 DB unique constraint를 근거로 동일한 Conflict 응답으로 변환한다.
			throw reportAlreadyExists();
		}
	}

	// 공개 플랜을 내 플랜으로 복사 (새 기간보다 긴 날짜의 일정은 버림)
	@Transactional
	public Long copyPlan(Long memberId, Long sourcePlanId, PlanCopyRequestDTO request) {
		// 일반 신규 플랜과 동일한 과거 시작일·날짜 범위·최대 14일 규칙을 복사본에도 적용한다.
		requestValidator.validateNewPlanDates(request.getStartDate(), request.getEndDate());
		PublishedPlanTargetDTO sourcePlan = requirePublishedPlan(sourcePlanId);

		long newPlanId = psd.nextTravelPlanId();
		psd.insertTravelPlan(
				newPlanId, sourcePlanId, memberId, request.getTitle().trim(),
				sourcePlan.getRegionCode(), request.getStartDate(), request.getEndDate()
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

	private PublishedPlanTargetDTO requirePublishedPlan(Long planId) {
		PublishedPlanTargetDTO target = psd.selectPublishedPlanTarget(planId);
		if (target == null) {
			throw planNotFound();
		}
		return target;
	}

	private BusinessException reportAlreadyExists() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"REPORT_ALREADY_EXISTS",
				"이미 신고한 여행 플랜입니다."
		);
	}

	private BusinessException planNotFound() {
		return new BusinessException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "공개 여행 플랜을 찾을 수 없습니다.");
	}
}
