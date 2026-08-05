package com.noblesi.travelplanner.admin.auth.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.auth.domain.AdminDomain;

@Mapper
public interface AdminDAO {

	AdminDomain loginAdmin(@Param("loginId") String loginId);
}
