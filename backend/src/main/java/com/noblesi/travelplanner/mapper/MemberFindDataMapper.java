package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.noblesi.travelplanner.dto.login.EmailFindRequest;
import com.noblesi.travelplanner.dto.login.MemberRewordPasswordRequest;
import com.noblesi.travelplanner.dto.login.PasswordFindRewordRequest;

@Mapper
public interface MemberFindDataMapper {
	String selectEmailFind(EmailFindRequest emailFindRequest);

	long selectPasswordFind(PasswordFindRewordRequest passwordFindRewordRequest);

	int updateRewordPassword(MemberRewordPasswordRequest memberRewordPasswordRequest);

}
