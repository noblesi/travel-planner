package com.noblesi.travelplanner.plansearch.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewTravelPlanDTO {

	private Long planId;
	private Long sourcePlanId;
	private Long memberId;
	private String title;
	private String regionCode;
	private LocalDate startDate;
	private LocalDate endDate;
}
