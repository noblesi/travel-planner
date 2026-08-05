package com.noblesi.travelplanner.admin.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.admin.auth.dao.AdminDAO;
import com.noblesi.travelplanner.admin.auth.domain.AdminDomain;
import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;
import com.noblesi.travelplanner.common.exception.BusinessException;

@Service
public class AdminAuthService {
	
	private final AdminDAO adminDAO;
	
	public AdminAuthService(AdminDAO adminDAO) {
		this.adminDAO = adminDAO;
	}

	public AdminDTO login(AdminDTO request) {

		AdminDomain adminDomain = adminDAO.loginAdmin(request.getLoginId());
		
		if(adminDomain == null) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, 
					"INVALID_ADMIN_LOGIN", 
					"관리자 아이디 또는 비밀번호가 올바르지 않습니다.");
		}//end if
		
		if(!adminDomain.getPassword().equals(request.getPassword())) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, 
					"INVALID_ADMIN_LOGIN", 
					"관리자 아이디 또는 비밀번호가 올바르지 않습니다.");
		}//end if
		
		AdminDTO adminDTO = new AdminDTO();
		adminDTO.setAdminId(adminDomain.getAdminId());
		adminDTO.setLoginId(adminDomain.getLoginId());
		adminDTO.setName(adminDomain.getName());
		adminDTO.setEmail(adminDomain.getEmail());
		adminDTO.setPhoneNumber(adminDomain.getPhoneNumber());
		adminDTO.setAdminRoleCode(adminDomain.getAdminRoleCode());
		adminDTO.setAdminStatus(adminDomain.getAdminStatus());
		adminDTO.setCreateAt(adminDomain.getCreateAt());
		
		return adminDTO;
	}//login
}
