package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.member.AuthenticatedMember;
import com.noblesi.travelplanner.dto.member.JoinMemberRequest;

@Mapper
public interface MemberMapper {

	/**
	 * 
	 * 유저의 개인정보 및 아이디와 비밀번호를 DB에 저장한다.
	 * 
	 * @param joinMemberRequest 저장할 유저 데이터
	 * @return 1 = 성공, 0 = 실패
	**/
	int insertMember(JoinMemberRequest joinMemberRequest);

	/**
	 * 
	 * 유저의 이메일이 DB에 존재하는지 확인한다.
	 * 
	 * @param email 확인할 이메일
	 * @return true = 존재, false = 존재하지 않음
	**/
	int selectEmailCnt(@Param("email") String email);

	AuthenticatedMember findForEmailAuthentication(@Param("email") String email);
}
