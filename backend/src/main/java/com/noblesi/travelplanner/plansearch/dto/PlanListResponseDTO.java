package com.noblesi.travelplanner.plansearch.dto;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

@Alias("PlanListResponseDTO")
@Getter
@Setter
public class PlanListResponseDTO {

	private String planId;
	private String title;
	private String region;
	private int days;
	private int likeCount;
	private int viewCount;
	private String authorName;
	private String authorImage;
	private String thumbnailImage;
}
