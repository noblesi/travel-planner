package com.noblesi.travelplanner.admin.tour.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.admin.tour.domain.TourSyncHistoryRecord;
import com.noblesi.travelplanner.admin.tour.dto.TourSyncHistoryDTO;
import com.noblesi.travelplanner.admin.tour.dto.TourSyncSummaryDTO;
import com.noblesi.travelplanner.domain.place.PlaceType;
import com.noblesi.travelplanner.integration.tourapi.TourApiClient;
import com.noblesi.travelplanner.integration.tourapi.TourApiException;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult;
import com.noblesi.travelplanner.mapper.PlaceMasterMapper;
import com.noblesi.travelplanner.mapper.RegionMapper;
import com.noblesi.travelplanner.service.PlaceCatalogService;

@Service
public class AdminTourSyncService {

	private static final Logger log = LoggerFactory.getLogger(AdminTourSyncService.class);
	private static final int PAGE_SIZE = 100;
	private static final int HISTORY_SIZE = 10;
	private static final Duration SYNC_LEASE_DURATION = Duration.ofMinutes(5);
	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

	private final TourApiClient tourApiClient;
	private final PlaceCatalogService placeCatalogService;
	private final PlaceMasterMapper placeMasterMapper;
	private final RegionMapper regionMapper;
	private final TourSyncExecutionStore executionStore;
	private final Clock clock;

	public AdminTourSyncService(
			TourApiClient tourApiClient,
			PlaceCatalogService placeCatalogService,
			PlaceMasterMapper placeMasterMapper,
			RegionMapper regionMapper,
			TourSyncExecutionStore executionStore,
			Clock clock
	) {
		this.tourApiClient = tourApiClient;
		this.placeCatalogService = placeCatalogService;
		this.placeMasterMapper = placeMasterMapper;
		this.regionMapper = regionMapper;
		this.executionStore = executionStore;
		this.clock = clock;
	}

	public TourSyncHistoryDTO synchronize(String manager) {
		OffsetDateTime startedAt = OffsetDateTime.now(clock);
		String syncId = "S-" + UUID.randomUUID();
		if (!executionStore.tryStart(syncId, manager, startedAt, leaseExpiresAt())) {
			throw new IllegalStateException("이미 TOUR API 동기화가 진행 중입니다.");
		}

		int changedCount = 0;
		int failedCount = 0;

		try {
			for (var region : regionMapper.findActiveSidoRegions()) {
				try {
					changedCount += synchronizeRegion(region.regionCode(), syncId);
				} catch (TourApiException exception) {
					failedCount++;
					if (exception.getReason() == TourApiException.Reason.NOT_CONFIGURED
							|| exception.getReason() == TourApiException.Reason.AUTHENTICATION_FAILED) {
						throw exception;
					}
				}
			}

			String status = failedCount == 0 ? "성공" : "부분 성공";
			TourSyncHistoryDTO result = createHistory(syncId, startedAt, changedCount, failedCount, status, manager);
			executionStore.complete(toRecord(result, startedAt));
			return result;
		} catch (RuntimeException exception) {
			TourSyncHistoryDTO result = createHistory(
					syncId,
					startedAt,
					changedCount,
					Math.max(1, failedCount),
					"실패",
					manager
			);
			try {
				executionStore.complete(toRecord(result, startedAt));
			} catch (RuntimeException historyException) {
				exception.addSuppressed(historyException);
				executionStore.release(syncId);
				log.error("TOUR API 동기화 실패 이력을 저장하지 못했습니다. syncId={}", syncId, historyException);
			}
			throw exception;
		}
	}

	public List<TourSyncSummaryDTO> getSummary() {
		List<TourSyncSummaryDTO> summary = new ArrayList<>();
		summary.add(new TourSyncSummaryDTO("전체", formatCount(placeMasterMapper.countActiveTourApiPlaces())));
		summary.add(new TourSyncSummaryDTO("관광지", formatCount(countType(PlaceType.ATTRACTION))));
		summary.add(new TourSyncSummaryDTO("음식점", formatCount(countType(PlaceType.RESTAURANT))));
		summary.add(new TourSyncSummaryDTO("숙박", formatCount(countType(PlaceType.ACCOMMODATION))));
		return summary;
	}

	public List<TourSyncHistoryDTO> getHistory() {
		return executionStore.getRecentHistory(HISTORY_SIZE).stream()
				.map(this::toDto)
				.toList();
	}

	public OffsetDateTime getLastSyncedAt() {
		return placeMasterMapper.findLastTourApiSyncedAt();
	}

	public boolean isSyncing() {
		return executionStore.isRunning(OffsetDateTime.now(clock));
	}

	private int synchronizeRegion(String regionCode, String syncId) {
		int page = 1;
		int changedCount = 0;

		while (true) {
			TourApiSearchResult result = tourApiClient.searchArea(regionCode, page, PAGE_SIZE);
			placeCatalogService.rememberTourApiPlaces(result.places());
			changedCount += result.places().size();
			executionStore.heartbeat(syncId, leaseExpiresAt());

			if ((long) page * result.size() >= result.totalCount() || result.places().isEmpty()) {
				break;
			}
			page++;
		}
		return changedCount;
	}

	private int countType(PlaceType placeType) {
		return placeMasterMapper.countActiveTourApiPlacesByType(placeType.name());
	}

	private String formatCount(int count) {
		return String.format("%,d", count);
	}

	private TourSyncHistoryDTO createHistory(
			String syncId,
			OffsetDateTime startedAt,
			int changedCount,
			int failedCount,
			String status,
			String manager
	) {
		String startedAtText = startedAt.atZoneSameInstant(KOREA_ZONE).format(DATE_FORMAT);
		return new TourSyncHistoryDTO(syncId, startedAtText, changedCount, failedCount, status, manager);
	}

	private OffsetDateTime leaseExpiresAt() {
		return OffsetDateTime.now(clock).plus(SYNC_LEASE_DURATION);
	}

	private TourSyncHistoryRecord toRecord(TourSyncHistoryDTO result, OffsetDateTime startedAt) {
		return new TourSyncHistoryRecord(
				result.id(),
				startedAt,
				result.changedCount(),
				result.failedCount(),
				result.status(),
				result.manager()
		);
	}

	private TourSyncHistoryDTO toDto(TourSyncHistoryRecord history) {
		return createHistory(
				history.syncId(),
				history.startedAt(),
				history.changedCount(),
				history.failedCount(),
				history.status(),
				history.manager()
		);
	}
}
