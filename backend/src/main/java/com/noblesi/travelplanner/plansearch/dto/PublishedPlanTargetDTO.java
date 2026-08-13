package com.noblesi.travelplanner.plansearch.dto;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

@Alias("PublishedPlanTargetDTO")
@Getter
@Setter
public class PublishedPlanTargetDTO {

	private Long ownerMemberId;
	private String regionCode;
}
