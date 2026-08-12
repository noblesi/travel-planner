package com.noblesi.travelplanner.admin.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;
import com.noblesi.travelplanner.admin.auth.service.AdminAuthService;
import com.noblesi.travelplanner.common.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

	private static final String LOGIN_ADMIN_SESSION_KEY = "loginAdmin";

	@Autowired
	private AdminAuthService adminAuthService;

	@GetMapping("/login")
	public String loginForm(Model model) {
		// 검증 실패 후 돌아온 DTO가 있으면 입력값을 유지하고, 최초 접근일 때만 새 DTO를 만듭니다.
		if (!model.containsAttribute("adminDTO")) {
			model.addAttribute("adminDTO", new AdminDTO());
		}
		return "admin/auth/adminLoginView";
	}

	@PostMapping("/login")
	public String login(@Valid @ModelAttribute("adminDTO") AdminDTO adminDTO, BindingResult bindingResult,
			HttpSession session, Model model) {
		// Bean Validation 오류가 있으면 인증을 시도하지 않고 로그인 화면을 다시 보여줍니다.
		if (bindingResult.hasErrors()) {
			return "admin/auth/adminLoginView";
		}

		try {
			// 인증된 관리자 정보만 세션에 저장하고 PRG 방식으로 대시보드에 이동합니다.
			AdminDTO loginAdmin = adminAuthService.login(adminDTO);
			session.setAttribute(LOGIN_ADMIN_SESSION_KEY, loginAdmin);
			return "redirect:/admin/dashboard";
		} catch (BusinessException exception) {
			model.addAttribute("loginError", "관리자 아이디 또는 비밀번호가 올바르지 않습니다.");
			return "admin/auth/adminLoginView";
		}
	}

	@PostMapping("/logout")
	public String logout(HttpServletRequest request) {
		// 기존 세션이 있을 때만 폐기하여 불필요한 새 세션 생성을 방지합니다.
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return "redirect:/admin/login";
	}
}
