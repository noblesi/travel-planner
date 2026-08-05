package com.noblesi.travelplanner.admin.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;
import com.noblesi.travelplanner.admin.auth.service.AdminAuthService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

	private final AdminAuthService adminAuthService;

	public AdminAuthController(AdminAuthService adminAuthService) {
		this.adminAuthService = adminAuthService;
	}

	@PostMapping("/login")
	public AdminDTO adminLogin(@RequestBody AdminDTO adminDTO, HttpSession session) {
		AdminDTO loginAdmin = adminAuthService.login(adminDTO);
		session.setAttribute("loginAdmin", loginAdmin);
		return loginAdmin;
	}
}
