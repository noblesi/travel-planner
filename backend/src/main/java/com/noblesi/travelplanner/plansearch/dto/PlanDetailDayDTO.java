package com.noblesi.travelplanner.plansearch.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanDetailDayDTO {

	private int dayNumber;
	private LocalDate visitDate;
	private List<PlanDetailPlaceDTO> places;
}
