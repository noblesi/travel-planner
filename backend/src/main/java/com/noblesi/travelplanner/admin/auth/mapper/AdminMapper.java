package com.noblesi.travelplanner.admin.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.auth.domain.AdminDomain;

@Mapper
public interface AdminMapper {

	AdminDomain loginAdmin(@Param("loginId") String loginId);
}
