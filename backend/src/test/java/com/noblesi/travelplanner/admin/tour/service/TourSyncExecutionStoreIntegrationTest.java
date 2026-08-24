package com.noblesi.travelplanner.admin.tour.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.noblesi.travelplanner.admin.tour.domain.TourSyncHistoryRecord;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_tour_sync_store;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@ActiveProfiles("local")
class TourSyncExecutionStoreIntegrationTest {

	@Autowired
	private TourSyncExecutionStore executionStore;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	@AfterEach
	void resetState() {
		jdbcTemplate.update("DELETE FROM TOUR_SYNC_HISTORY");
		jdbcTemplate.update("""
				UPDATE TOUR_SYNC_STATE
				   SET RUNNING_YN = 'N', SYNC_ID = NULL, STARTED_AT = NULL,
				       LEASE_EXPIRES_AT = NULL, STARTED_BY = NULL
				 WHERE LOCK_ID = 1
				""");
	}

	@Test
	void allowsOnlyOneConcurrentLeaseOwner() throws Exception {
		OffsetDateTime startedAt = OffsetDateTime.of(2026, 8, 23, 10, 0, 0, 0, ZoneOffset.UTC);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			java.util.function.Function<String, java.util.concurrent.Callable<Boolean>> acquire = syncId -> () -> {
				ready.countDown();
				start.await();
				return executionStore.tryStart(
						syncId,
						"admin",
						startedAt,
						startedAt.plusMinutes(5)
				);
			};
			Future<Boolean> first = executor.submit(acquire.apply("S-FIRST"));
			Future<Boolean> second = executor.submit(acquire.apply("S-SECOND"));
			ready.await();
			start.countDown();

			assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
		}
		assertThat(executionStore.isRunning(startedAt.plusMinutes(1))).isTrue();
	}

	@Test
	void persistsHistoryAndAllowsNextExecutionAfterCompletion() {
		OffsetDateTime startedAt = OffsetDateTime.of(2026, 8, 23, 10, 0, 0, 0, ZoneOffset.UTC);
		assertThat(executionStore.tryStart(
				"S-COMPLETE",
				"admin",
				startedAt,
				startedAt.plusMinutes(5)
		)).isTrue();

		executionStore.complete(new TourSyncHistoryRecord(
				"S-COMPLETE",
				startedAt,
				12,
				0,
				"성공",
				"admin"
		));

		assertThat(executionStore.isRunning(startedAt.plusMinutes(1))).isFalse();
		assertThat(executionStore.getRecentHistory(10))
				.singleElement()
				.satisfies(history -> {
					assertThat(history.syncId()).isEqualTo("S-COMPLETE");
					assertThat(history.changedCount()).isEqualTo(12);
				});
		assertThat(executionStore.tryStart(
				"S-NEXT",
				"admin",
				startedAt.plusMinutes(1),
				startedAt.plusMinutes(6)
		)).isTrue();
	}
}
