package com.noblesi.travelplanner.admin.member.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.member.dto.AdminMemberDetailDTO;
import com.noblesi.travelplanner.admin.member.dto.AdminMemberListDTO;
import com.noblesi.travelplanner.admin.member.dto.AdminMemberSearchDTO;

@Mapper
public interface AdminMemberMapper {

	long countMemberList(@Param("search") AdminMemberSearchDTO search);

	List<AdminMemberListDTO> selectMemberList(@Param("search") AdminMemberSearchDTO search);

	AdminMemberDetailDTO selectMemberDetail(
			
			@Param("memberId") String MemberID
			
	);
	
	int updateStatus(
		
			@Param("memberId") String memberId,
			@Param("memberStatus") String memberStatus
	);

}// AdminMemberMapper
