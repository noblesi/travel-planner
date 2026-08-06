package com.noblesi.travelplanner.admin.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.admin.auth.dao.AdminDAO;
import com.noblesi.travelplanner.admin.auth.domain.AdminDomain;
import com.noblesi.travelplanner.admin.auth.dto.AdminLoginRequest;
import com.noblesi.travelplanner.admin.auth.security.AdminPrincipal;
import com.noblesi.travelplanner.common.exception.BusinessException;

@Service
public class AdminAuthService {
	
	private static final String ACTIVE_STATUS = "ACTIVE";

	private final AdminDAO adminDAO;
	private final PasswordEncoder passwordEncoder;

	public AdminAuthService(AdminDAO adminDAO, PasswordEncoder passwordEncoder) {
		this.adminDAO = adminDAO;
		this.passwordEncoder = passwordEncoder;
	}

	public AdminPrincipal authenticate(AdminLoginRequest request) {
		AdminDomain adminDomain = adminDAO.loginAdmin(request.loginId());
		if (adminDomain == null
				|| !ACTIVE_STATUS.equals(adminDomain.getAdminStatus())
				|| !passwordEncoder.matches(request.password(), adminDomain.getPassword())) {
			throw invalidCredentials();
		}

		return new AdminPrincipal(
				adminDomain.getAdminId(),
				adminDomain.getLoginId(),
				adminDomain.getName(),
				adminDomain.getEmail(),
				adminDomain.getAdminRoleCode()
		);
	}

	private BusinessException invalidCredentials() {
		return new BusinessException(
				HttpStatus.UNAUTHORIZED,
				"INVALID_ADMIN_LOGIN",
				"관리자 아이디 또는 비밀번호가 올바르지 않습니다."
		);
	}
}
