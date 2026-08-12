package com.noblesi.travelplanner.plansearch.dao;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

@Alias("PlanScheduleCopyRowDTO")
@Getter
@Setter
public class PlanScheduleCopyRowDTO {

	private int dayNumber;
	private String timeSlot;
	private int positionNo;
	private String placeProvider;
	private String externalPlaceId;
	private String placeName;
	private String categoryName;
	private String address;
	private String latitude;
	private String longitude;
	private String imageUrl;
	private String description;
}
