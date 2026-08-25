package com.noblesi.travelplanner.mapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TravelPlanDateMapper {

	int updateTravelDates(
			@Param("planId") long planId,
			@Param("memberId") long memberId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("versionNo") int versionNo,
			@Param("updatedAt") OffsetDateTime updatedAt
	);
}
