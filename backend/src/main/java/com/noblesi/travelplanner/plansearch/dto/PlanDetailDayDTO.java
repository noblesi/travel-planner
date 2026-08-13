package com.noblesi.travelplanner.plansearch.dto;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

@Alias("PlanDetailDayDTO")
@Getter
@Setter
public class PlanDetailDayDTO {

	private int dayNumber;
	private LocalDate visitDate;
	private List<PlanDetailPlaceDTO> places;
}
