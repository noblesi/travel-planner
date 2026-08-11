package com.noblesi.travelplanner.admin.dashboard.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.noblesi.travelplanner.admin.dashboard.domain.PopularRegionStatDomain;
import com.noblesi.travelplanner.admin.dashboard.domain.WeeklyPlanStatDomain;

@Mapper
public interface AdminDashboardDAO {

	int selectTotalMemberCount();

	int selectNewMemberCount();

	int selectPublicPlanCount();

	int selectPendingReportCount();

	List<WeeklyPlanStatDomain> selectWeeklyPlanStats();

	List<PopularRegionStatDomain> selectPopularRegionStats();
}
