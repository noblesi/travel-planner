package com.noblesi.travelplanner.admin.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.admin.auth.mapper.AdminMapper;
import com.noblesi.travelplanner.admin.auth.domain.AdminDomain;
import com.noblesi.travelplanner.admin.auth.security.AdminPrincipal;
import com.noblesi.travelplanner.common.exception.BusinessException;

@Service
public class AdminAuthService {

	private static final String ACTIVE_STATUS = "ACTIVE";

	@Autowired
	private AdminMapper adminMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public AdminPrincipal authenticate(String loginId, String password) {
		AdminDomain adminDomain = adminMapper.loginAdmin(loginId);
		if (adminDomain == null || !ACTIVE_STATUS.equals(adminDomain.getAdminStatus())
				|| !passwordEncoder.matches(password, adminDomain.getPassword())) {
			throw invalidCredentials();
		}

		return new AdminPrincipal(
				adminDomain.getAdminId(),
				adminDomain.getLoginId(),
				adminDomain.getName(),
				adminDomain.getAdminRoleCode()
		);
	}

	private BusinessException invalidCredentials() {
		return new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_ADMIN_LOGIN", "관리자 아이디 또는 비밀번호가 올바르지 않습니다.");
	}
}
