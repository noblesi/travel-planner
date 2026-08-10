package com.noblesi.travelplanner.plansearch.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanCopyRequestDTO {

	private String title;
	private LocalDate startDate;
	private LocalDate endDate;
}
