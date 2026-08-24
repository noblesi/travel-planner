package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TravelPlanDerivedDataMapper {

	int incrementPublishedPlanViewCount(@Param("planId") long planId);

	int updateThumbnailWithoutVersion(
			@Param("planId") long planId,
			@Param("thumbnailImageUrl") String thumbnailImageUrl
	);
}
