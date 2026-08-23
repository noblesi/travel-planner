package com.noblesi.travelplanner.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.tour.domain.TourSyncHistoryRecord;

@Mapper
public interface TourSyncJobMapper {

	int tryAcquire(
			@Param("syncId") String syncId,
			@Param("manager") String manager,
			@Param("startedAt") OffsetDateTime startedAt,
			@Param("leaseExpiresAt") OffsetDateTime leaseExpiresAt
	);

	int heartbeat(
			@Param("syncId") String syncId,
			@Param("leaseExpiresAt") OffsetDateTime leaseExpiresAt
	);

	int release(@Param("syncId") String syncId);

	int insertHistory(TourSyncHistoryRecord history);

	List<TourSyncHistoryRecord> findRecentHistory(@Param("limit") int limit);

	int countRunning(@Param("now") OffsetDateTime now);
}
