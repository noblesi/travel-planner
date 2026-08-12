package com.noblesi.travelplanner.plansearch.dao;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

@Alias("PlanScheduleRowDTO")
@Getter
@Setter
public class PlanScheduleRowDTO {

	private int dayNumber;
	private String timeSlot;
	private String placeName;
	private String address;
	private double latitude;
	private double longitude;
}
