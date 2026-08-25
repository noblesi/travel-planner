package com.noblesi.travelplanner.service;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthService {

	private final JdbcTemplate jdbcTemplate;

	public SystemHealthService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean isDatabaseReady() {
		try {
			Integer result = jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);
			return Integer.valueOf(1).equals(result);
		} catch (DataAccessException exception) {
			return false;
		}
	}
}
