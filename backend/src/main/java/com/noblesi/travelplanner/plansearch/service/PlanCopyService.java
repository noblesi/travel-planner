package com.noblesi.travelplanner.plansearch.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.plansearch.dao.PlanSearchDAO;
import com.noblesi.travelplanner.plansearch.dto.NewTravelPlanDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanCopyRequestDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanScheduleCopyRowDTO;
import com.noblesi.travelplanner.plansearch.dto.PublishedPlanTargetDTO;
import com.noblesi.travelplanner.service.TravelPlanRequestValidator;

@Service
public class PlanCopyService {

	private final PlanSearchDAO planSearchDAO;
	private final PublishedPlanResolver publishedPlanResolver;
	private final TravelPlanRequestValidator requestValidator;

	public PlanCopyService(
			PlanSearchDAO planSearchDAO,
			PublishedPlanResolver publishedPlanResolver,
			TravelPlanRequestValidator requestValidator
	) {
		this.planSearchDAO = planSearchDAO;
		this.publishedPlanResolver = publishedPlanResolver;
		this.requestValidator = requestValidator;
	}

	@Transactional
	public long copy(long memberId, long sourcePlanId, PlanCopyRequestDTO request) {
		requestValidator.validateNewPlanDates(request.getStartDate(), request.getEndDate());
		PublishedPlanTargetDTO sourcePlan = publishedPlanResolver.require(sourcePlanId);

		long newPlanId = planSearchDAO.nextTravelPlanId();
		NewTravelPlanDTO newPlan = new NewTravelPlanDTO();
		newPlan.setPlanId(newPlanId);
		newPlan.setSourcePlanId(sourcePlanId);
		newPlan.setMemberId(memberId);
		newPlan.setTitle(request.getTitle().trim());
		newPlan.setRegionCode(sourcePlan.getRegionCode());
		newPlan.setStartDate(request.getStartDate());
		newPlan.setEndDate(request.getEndDate());
		planSearchDAO.insertTravelPlan(newPlan);
		planSearchDAO.insertPlanMember(newPlanId, memberId);

		Map<Integer, Long> newDayIdByDayNo = createDays(newPlanId, request);
		for (PlanScheduleCopyRowDTO row : planSearchDAO.selectPlanScheduleForCopy(sourcePlanId)) {
			Long newPlanDayId = newDayIdByDayNo.get(row.getDayNumber());
			if (newPlanDayId != null) {
				planSearchDAO.insertScheduleItem(planSearchDAO.nextScheduleItemId(), newPlanDayId, row);
			}
		}
		return newPlanId;
	}

	private Map<Integer, Long> createDays(long planId, PlanCopyRequestDTO request) {
		Map<Integer, Long> dayIdByDayNo = new HashMap<>();
		LocalDate travelDate = request.getStartDate();
		int dayNo = 1;
		while (!travelDate.isAfter(request.getEndDate())) {
			long planDayId = planSearchDAO.nextPlanDayId();
			planSearchDAO.insertPlanDay(planDayId, planId, dayNo, travelDate);
			dayIdByDayNo.put(dayNo, planDayId);
			travelDate = travelDate.plusDays(1);
			dayNo++;
		}
		return dayIdByDayNo;
	}
}
