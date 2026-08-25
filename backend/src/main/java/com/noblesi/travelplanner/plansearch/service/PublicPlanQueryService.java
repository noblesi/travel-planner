package com.noblesi.travelplanner.plansearch.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.api.PageResponse;
import com.noblesi.travelplanner.common.api.Pagination;
import com.noblesi.travelplanner.mapper.TravelPlanDerivedDataMapper;
import com.noblesi.travelplanner.plansearch.dao.PlanLikeDAO;
import com.noblesi.travelplanner.plansearch.dao.PlanSearchDAO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailDayDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailPlaceDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanListResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanScheduleRowDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanSearchRequestDTO;

@Service
public class PublicPlanQueryService {

	private static final int MAX_SIZE = 100;

	private final PlanSearchDAO planSearchDAO;
	private final PlanLikeDAO planLikeDAO;
	private final TravelPlanDerivedDataMapper derivedDataMapper;
	private final PublishedPlanResolver publishedPlanResolver;

	public PublicPlanQueryService(
			PlanSearchDAO planSearchDAO,
			PlanLikeDAO planLikeDAO,
			TravelPlanDerivedDataMapper derivedDataMapper,
			PublishedPlanResolver publishedPlanResolver
	) {
		this.planSearchDAO = planSearchDAO;
		this.planLikeDAO = planLikeDAO;
		this.derivedDataMapper = derivedDataMapper;
		this.publishedPlanResolver = publishedPlanResolver;
	}

	@Transactional(readOnly = true)
	public PageResponse<PlanListResponseDTO> search(PlanSearchRequestDTO request) {
		PlanSearchRequestDTO normalizedRequest = normalize(request);
		long totalCount = planSearchDAO.countPlanList(normalizedRequest);
		Pagination pagination = Pagination.of(
				normalizedRequest.getPage(),
				normalizedRequest.getSize(),
				totalCount
		);
		if (totalCount == 0) {
			return PageResponse.empty(pagination);
		}
		return PageResponse.of(planSearchDAO.selectPlanList(normalizedRequest), pagination);
	}

	@Transactional
	public PlanDetailResponseDTO getDetail(long planId, Long memberId, boolean increaseViewCount) {
		if (increaseViewCount && derivedDataMapper.incrementPublishedPlanViewCount(planId) != 1) {
			throw publishedPlanResolver.planNotFound();
		}
		PlanDetailResponseDTO detail = planSearchDAO.selectPlanById(planId);
		if (detail == null) {
			throw publishedPlanResolver.planNotFound();
		}
		detail.setLiked(memberId != null && planLikeDAO.existsLike(memberId, planId));
		detail.setDays(loadDays(planId));
		return detail;
	}

	private PlanSearchRequestDTO normalize(PlanSearchRequestDTO request) {
		PlanSearchRequestDTO normalized = new PlanSearchRequestDTO();
		normalized.setKeyword(request.getKeyword() == null ? "" : request.getKeyword().trim());
		normalized.setPage(Math.max(request.getPage(), 1));
		normalized.setSize(Math.min(Math.max(request.getSize(), 1), MAX_SIZE));
		return normalized;
	}

	private List<PlanDetailDayDTO> loadDays(long planId) {
		List<PlanDetailDayDTO> days = planSearchDAO.selectPlanDays(planId);
		Map<Integer, List<PlanDetailPlaceDTO>> placesByDay = new HashMap<>();
		for (PlanScheduleRowDTO row : planSearchDAO.selectPlanSchedule(planId)) {
			PlanDetailPlaceDTO place = new PlanDetailPlaceDTO();
			place.setTimeSlot(row.getTimeSlot());
			place.setPlaceName(row.getPlaceName());
			place.setAddress(row.getAddress());
			place.setLatitude(row.getLatitude());
			place.setLongitude(row.getLongitude());
			placesByDay.computeIfAbsent(row.getDayNumber(), ignored -> new ArrayList<>()).add(place);
		}
		for (PlanDetailDayDTO day : days) {
			day.setPlaces(placesByDay.getOrDefault(day.getDayNumber(), List.of()));
		}
		return days;
	}
}
