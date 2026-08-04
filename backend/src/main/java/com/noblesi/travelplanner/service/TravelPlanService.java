package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.dto.plan.CreateTravelPlanRequest;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanDatesRequest;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanMetadataRequest;

@Service
public class TravelPlanService {

	private final TravelPlanCreationService creationService;
	private final PlanEditorQueryService editorQueryService;
	private final TravelPlanMetadataService metadataService;
	private final TravelPlanDateService dateService;

	public TravelPlanService(
			TravelPlanCreationService creationService,
			PlanEditorQueryService editorQueryService,
			TravelPlanMetadataService metadataService,
			TravelPlanDateService dateService
	) {
		this.creationService = creationService;
		this.editorQueryService = editorQueryService;
		this.metadataService = metadataService;
		this.dateService = dateService;
	}

	public CreateTravelPlanResponse createTravelPlan(CreateTravelPlanRequest request) {
		return creationService.create(request);
	}

	public PlanEditorResponse getPlanEditor(String planIdValue) {
		return editorQueryService.getPlanEditor(planIdValue);
	}

	public PlanEditorResponse updateTravelPlanMetadata(
			String planIdValue,
			UpdateTravelPlanMetadataRequest request
	) {
		return metadataService.update(planIdValue, request);
	}

	public PlanEditorResponse updateTravelPlanDates(
			String planIdValue,
			UpdateTravelPlanDatesRequest request
	) {
		return dateService.update(planIdValue, request);
	}
}
