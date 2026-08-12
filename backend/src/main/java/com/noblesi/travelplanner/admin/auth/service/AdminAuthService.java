package com.noblesi.travelplanner.admin.auth.service;

import java.sql.Date;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.admin.auth.mapper.AdminMapper;
import com.noblesi.travelplanner.admin.auth.domain.AdminDomain;
import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;
import com.noblesi.travelplanner.common.exception.BusinessException;

@Service
public class AdminAuthService {

	private static final String ACTIVE_STATUS = "ACTIVE";

	private final AdminMapper adminMapper;
	private final PasswordEncoder passwordEncoder;

	public AdminAuthService(AdminMapper adminMapper, PasswordEncoder passwordEncoder) {
		this.adminMapper = adminMapper;
		this.passwordEncoder = passwordEncoder;
	}

	public AdminDTO login(AdminDTO request) {
		AdminDomain adminDomain = adminMapper.loginAdmin(request.getLoginId());
		if (adminDomain == null || !ACTIVE_STATUS.equals(adminDomain.getAdminStatus())
				|| !passwordEncoder.matches(request.getPassword(), adminDomain.getPassword())) {
			throw invalidCredentials();
		}

		AdminDTO loginAdmin = new AdminDTO();
		loginAdmin.setAdminId(Math.toIntExact(adminDomain.getAdminId()));
		loginAdmin.setLoginId(adminDomain.getLoginId());
		loginAdmin.setName(adminDomain.getName());
		loginAdmin.setEmail(adminDomain.getEmail());
		loginAdmin.setPhoneNumber(adminDomain.getPhoneNumber());
		loginAdmin.setAdminRoleCode(adminDomain.getAdminRoleCode());
		loginAdmin.setAdminStatus(adminDomain.getAdminStatus());
		if (adminDomain.getCreatedAt() != null) {
			loginAdmin.setCreateAt(Date.valueOf(adminDomain.getCreatedAt().toLocalDate()));
		}
		return loginAdmin;
	}

	private BusinessException invalidCredentials() {
		return new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_ADMIN_LOGIN", "관리자 아이디 또는 비밀번호가 올바르지 않습니다.");
	}
}
