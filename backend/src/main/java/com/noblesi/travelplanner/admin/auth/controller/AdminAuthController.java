package com.noblesi.travelplanner.admin.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;
import com.noblesi.travelplanner.admin.auth.security.AdminPrincipal;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

	@GetMapping("/login")
	public String loginForm(
			@RequestParam(required = false) String error,
			Authentication authentication,
			Model model
	) {
		if (authentication != null && authentication.getPrincipal() instanceof AdminPrincipal) {
			return "redirect:/admin/dashboard";
		}
		if (!model.containsAttribute("adminDTO")) {
			model.addAttribute("adminDTO", new AdminDTO());
		}
		if (error != null) {
			model.addAttribute("loginError", "관리자 아이디 또는 비밀번호가 올바르지 않습니다.");
		}
		return "admin/auth/adminLoginView";
	}
}
