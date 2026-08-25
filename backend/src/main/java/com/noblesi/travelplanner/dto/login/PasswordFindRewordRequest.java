package com.noblesi.travelplanner.dto.login;

import java.time.LocalDate;

import org.apache.ibatis.type.Alias;

import com.fasterxml.jackson.annotation.JsonFormat;


@Alias("passwordFindRewordRequest")
public record PasswordFindRewordRequest(
		String email,
		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate birthDate,
		String phoneNumber
) {

}
