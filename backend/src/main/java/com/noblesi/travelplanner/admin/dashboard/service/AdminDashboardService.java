package com.noblesi.travelplanner.admin.dashboard.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.admin.dashboard.mapper.AdminDashboardMapper;
import com.noblesi.travelplanner.admin.dashboard.domain.PopularRegionStatDomain;
import com.noblesi.travelplanner.admin.dashboard.domain.WeeklyPlanStatDomain;
import com.noblesi.travelplanner.admin.dashboard.dto.AdminDashboardDTO;
import com.noblesi.travelplanner.admin.dashboard.dto.PopularRegionStatDTO;
import com.noblesi.travelplanner.admin.dashboard.dto.WeeklyPlanStatDTO;

@Service
public class AdminDashboardService {

	private final AdminDashboardMapper adminDashboardMapper;

	public AdminDashboardService(AdminDashboardMapper adminDashboardMapper) {
		this.adminDashboardMapper = adminDashboardMapper;
	}

	public AdminDashboardDTO getDashboard() {
		AdminDashboardDTO dashboard = new AdminDashboardDTO();
		dashboard.setTotalMemberCount(adminDashboardMapper.selectTotalMemberCount());
		dashboard.setNewMemberCount(adminDashboardMapper.selectNewMemberCount());
		dashboard.setPublicPlanCount(adminDashboardMapper.selectPublicPlanCount());
		dashboard.setPendingReportCount(adminDashboardMapper.selectPendingReportCount());
		dashboard.setWeeklyPlanStats(toWeeklyPlanStatDTOs(adminDashboardMapper.selectWeeklyPlanStats()));
		dashboard.setPopularRegionStats(toPopularRegionStatDTOs(adminDashboardMapper.selectPopularRegionStats()));
		return dashboard;
	}

	private List<WeeklyPlanStatDTO> toWeeklyPlanStatDTOs(List<WeeklyPlanStatDomain> domains) {
		List<WeeklyPlanStatDTO> result = new ArrayList<>();

		for (WeeklyPlanStatDomain domain : domains) {
			WeeklyPlanStatDTO dto = new WeeklyPlanStatDTO();
			dto.setDay(domain.getDay());
			dto.setValue(domain.getValue());
			result.add(dto);
		}

		return result;
	}

	private List<PopularRegionStatDTO> toPopularRegionStatDTOs(List<PopularRegionStatDomain> domains) {
		List<PopularRegionStatDTO> result = new ArrayList<>();
		int maximumCount = domains.isEmpty() ? 0 : domains.get(0).getCount();

		for (int index = 0; index < domains.size(); index++) {
			PopularRegionStatDomain domain = domains.get(index);
			PopularRegionStatDTO dto = new PopularRegionStatDTO();
			dto.setRank(index + 1);
			dto.setName(domain.getName());
			dto.setCount(domain.getCount());
			dto.setPercentage(maximumCount == 0 ? 0 : domain.getCount() * 100 / maximumCount);
			result.add(dto);
		}

		return result;
	}
}
