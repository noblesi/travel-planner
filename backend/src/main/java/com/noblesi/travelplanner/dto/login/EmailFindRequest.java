package com.noblesi.travelplanner.dto.login;

import java.sql.Date;
import java.time.LocalDate;

import org.apache.ibatis.type.Alias;

import com.fasterxml.jackson.annotation.JsonFormat;


@Alias("emailfindRequest")
public record EmailFindRequest(
		String memberName,
		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate birthDate,
		String phoneNumber
) {

}
