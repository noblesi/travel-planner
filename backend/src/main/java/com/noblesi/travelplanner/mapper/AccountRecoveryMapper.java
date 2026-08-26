package com.noblesi.travelplanner.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.dto.account.FindEmailRequest;
import com.noblesi.travelplanner.dto.account.VerifyPasswordRecoveryRequest;

@Mapper
public interface AccountRecoveryMapper {

	Optional<String> findActiveEmail(@Param("request") FindEmailRequest request);

	Optional<Long> findActiveMemberId(@Param("request") VerifyPasswordRecoveryRequest request);

	Optional<String> findActivePasswordHash(@Param("memberId") long memberId);

	int updateActivePassword(
			@Param("memberId") long memberId,
			@Param("passwordHash") String passwordHash
	);
}
