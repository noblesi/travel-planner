package com.noblesi.travelplanner.plansearch.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanSearchRequestDTO {

	private String keyword;
	private int page;
	private int size;
}
