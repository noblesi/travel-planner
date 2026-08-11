package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.common.exception.BusinessException;

@Component
class PositiveIdParser {

	long parse(String value, String parameterName) {
		if (value == null || !value.matches("[1-9]\\d*")) {
			throw invalidPathParameter(parameterName);
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException exception) {
			throw invalidPathParameter(parameterName);
		}
	}

	private BusinessException invalidPathParameter(String parameterName) {
		return new BusinessException(
				HttpStatus.BAD_REQUEST,
				"INVALID_PATH_PARAMETER",
				parameterName + "는 1 이상의 숫자여야 합니다."
		);
	}
}
