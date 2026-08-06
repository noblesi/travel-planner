package com.noblesi.travelplanner.admin.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;
import com.noblesi.travelplanner.admin.auth.service.AdminAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

	private static final String LOGIN_ADMIN_SESSION_KEY = "loginAdmin";

	private final AdminAuthService adminAuthService;

	public AdminAuthController(AdminAuthService adminAuthService) {
		this.adminAuthService = adminAuthService;
	}

	@PostMapping("/login")
	public AdminDTO login(@Valid @RequestBody AdminDTO adminDTO, HttpSession session) {
		AdminDTO loginAdmin = adminAuthService.login(adminDTO);
		session.setAttribute(LOGIN_ADMIN_SESSION_KEY, loginAdmin);
		return loginAdmin;
	}

	@GetMapping("/session")
	public AdminDTO getSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}

		Object loginAdmin = session.getAttribute(LOGIN_ADMIN_SESSION_KEY);
		return loginAdmin instanceof AdminDTO admin ? admin : null;
	}

	@PostMapping("/logout")
	public void logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}
}
