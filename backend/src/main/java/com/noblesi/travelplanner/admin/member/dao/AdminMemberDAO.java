package com.noblesi.travelplanner.admin.member.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.member.dto.AdminMemberDetailDTO;
import com.noblesi.travelplanner.admin.member.dto.AdminMemberListDTO;

@Mapper
public interface AdminMemberDAO {

	List<AdminMemberListDTO> selectMemberList(

			@Param("keyword") String keyword, 
			@Param("memberStatus") String memberStatus
			
	);

	AdminMemberDetailDTO selectMemberDetail(
			
			@Param("memberId") String MemberID
			
	);
	
	int updateStatus(
		
			@Param("memberId") String memberId,
			@Param("memberStatus") String memberStatus
	);

}// AdminMemberDAO
