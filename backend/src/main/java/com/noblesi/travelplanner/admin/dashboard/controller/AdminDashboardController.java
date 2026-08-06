package com.noblesi.travelplanner.admin.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.admin.dashboard.dto.AdminDashboardDTO;
import com.noblesi.travelplanner.admin.dashboard.service.AdminDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

	private final AdminDashboardService adminDashboardService;

	public AdminDashboardController(AdminDashboardService adminDashboardService) {
		this.adminDashboardService = adminDashboardService;
	}

	@GetMapping
	public AdminDashboardDTO getDashboard() {
		return adminDashboardService.getDashboard();
	}
}
