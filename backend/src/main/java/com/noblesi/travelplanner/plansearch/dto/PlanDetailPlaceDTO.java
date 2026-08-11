package com.noblesi.travelplanner.plansearch.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanDetailPlaceDTO {

	private String timeSlot;
	private String placeName;
	private String address;
	private double latitude;
	private double longitude;
}
