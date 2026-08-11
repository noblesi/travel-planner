package com.noblesi.travelplanner.plansearch.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanDetailResponseDTO {

	private String planId;
	private String title;
	private String authorName;
	private LocalDate startDate;
	private LocalDate endDate;
	private int likeCount;
	private int viewCount;
	private boolean liked;
	private List<PlanDetailDayDTO> days;
}
