package com.noblesi.travelplanner.admin.dashboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.noblesi.travelplanner.admin.dashboard.domain.PopularRegionStatDomain;
import com.noblesi.travelplanner.admin.dashboard.domain.WeeklyPlanStatDomain;

@Mapper
public interface AdminDashboardMapper {

	int selectTotalMemberCount();

	int selectNewMemberCount();

	int selectPublicPlanCount();

	int selectPendingReportCount();

	List<WeeklyPlanStatDomain> selectWeeklyPlanStats();

	List<PopularRegionStatDomain> selectPopularRegionStats();
}
