package com.noblesi.travelplanner.admin.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.noblesi.travelplanner.admin.dashboard.service.AdminDashboardService;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

	@Autowired
	private AdminDashboardService adminDashboardService;

	@GetMapping({"", "/dashboard"})
	public String getDashboard(Model model) {
		// Service 조회 결과를 Model에 담아 Thymeleaf 대시보드에서 사용합니다.
		model.addAttribute("pageTitle", "대시보드");
		model.addAttribute("dashboard", adminDashboardService.getDashboard());
		return "admin/dashboard/adminDashboardView";
	}
}
