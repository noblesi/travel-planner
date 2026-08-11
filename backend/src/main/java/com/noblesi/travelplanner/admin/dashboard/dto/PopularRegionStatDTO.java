package com.noblesi.travelplanner.admin.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PopularRegionStatDTO {

	private int rank;
	private String name;
	private int count;
	private int percentage;
}
