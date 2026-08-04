package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.member.AuthenticatedMember;

@Mapper
public interface MemberMapper {

	AuthenticatedMember findForEmailAuthentication(@Param("email") String email);
}
