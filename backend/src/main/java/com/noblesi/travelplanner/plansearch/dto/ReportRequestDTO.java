package com.noblesi.travelplanner.plansearch.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequestDTO {

	private Long planId;
	private String reason;
	private String detail;
}
