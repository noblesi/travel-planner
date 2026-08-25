package com.noblesi.travelplanner.admin.tour.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.admin.tour.domain.TourSyncHistoryRecord;
import com.noblesi.travelplanner.mapper.TourSyncJobMapper;

@Service
class TourSyncExecutionStore {

	private final TourSyncJobMapper mapper;

	TourSyncExecutionStore(TourSyncJobMapper mapper) {
		this.mapper = mapper;
	}

	@Transactional
	boolean tryStart(
			String syncId,
			String manager,
			OffsetDateTime startedAt,
			OffsetDateTime leaseExpiresAt
	) {
		return mapper.tryAcquire(syncId, manager, startedAt, leaseExpiresAt) == 1;
	}

	@Transactional
	void heartbeat(String syncId, OffsetDateTime leaseExpiresAt) {
		if (mapper.heartbeat(syncId, leaseExpiresAt) != 1) {
			throw new IllegalStateException("TOUR API 동기화 실행 권한이 만료되었습니다.");
		}
	}

	@Transactional
	void complete(TourSyncHistoryRecord history) {
		if (mapper.insertHistory(history) != 1) {
			throw new IllegalStateException("TOUR API 동기화 이력을 저장하지 못했습니다.");
		}
		mapper.release(history.syncId());
	}

	@Transactional
	void release(String syncId) {
		mapper.release(syncId);
	}

	@Transactional(readOnly = true)
	List<TourSyncHistoryRecord> getRecentHistory(int limit) {
		return mapper.findRecentHistory(limit);
	}

	@Transactional(readOnly = true)
	boolean isRunning(OffsetDateTime now) {
		return mapper.countRunning(now) == 1;
	}
}
