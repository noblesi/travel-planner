package com.noblesi.travelplanner.plansearch.dto;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

@Alias("PlanDetailResponseDTO")
@Getter
@Setter
public class PlanDetailResponseDTO {

	private Long planId;
	private String title;
	private String authorName;
	private LocalDate startDate;
	private LocalDate endDate;
	private int likeCount;
	private int viewCount;
	private boolean liked;
	private List<PlanDetailDayDTO> days;
}
