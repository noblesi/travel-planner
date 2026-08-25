package com.noblesi.travelplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SystemHealthServiceTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@InjectMocks
	private SystemHealthService systemHealthService;

	@Test
	void reportsReadyWhenDatabaseQuerySucceeds() {
		when(jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class)).thenReturn(1);

		assertThat(systemHealthService.isDatabaseReady()).isTrue();
	}

	@Test
	void reportsNotReadyWhenDatabaseQueryReturnsUnexpectedResult() {
		when(jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class)).thenReturn(null);

		assertThat(systemHealthService.isDatabaseReady()).isFalse();
	}

	@Test
	void reportsNotReadyWhenDatabaseQueryFails() {
		when(jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class))
				.thenThrow(new DataAccessResourceFailureException("database unavailable"));

		assertThat(systemHealthService.isDatabaseReady()).isFalse();
	}
}
