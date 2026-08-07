package com.noblesi.travelplanner.plansearch.dao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanScheduleRowDTO {

	private int dayNumber;
	private String timeSlot;
	private String placeName;
	private String description;
	private double latitude;
	private double longitude;
}
