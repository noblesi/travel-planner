package com.noblesi.travelplanner.admin.tour.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

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

	private static final int PAGE_SIZE = 100;
	private static final int HISTORY_SIZE = 10;
	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

	private final TourApiClient tourApiClient;
	private final PlaceCatalogService placeCatalogService;
	private final PlaceMasterMapper placeMasterMapper;
	private final RegionMapper regionMapper;
	private final Clock clock;
	private final AtomicBoolean syncing = new AtomicBoolean(false);
	private final Deque<TourSyncHistoryDTO> history = new ArrayDeque<>();

	public AdminTourSyncService(
			TourApiClient tourApiClient,
			PlaceCatalogService placeCatalogService,
			PlaceMasterMapper placeMasterMapper,
			RegionMapper regionMapper,
			Clock clock
	) {
		this.tourApiClient = tourApiClient;
		this.placeCatalogService = placeCatalogService;
		this.placeMasterMapper = placeMasterMapper;
		this.regionMapper = regionMapper;
		this.clock = clock;
	}

	public TourSyncHistoryDTO synchronize(String manager) {
		if (!syncing.compareAndSet(false, true)) {
			throw new IllegalStateException("이미 TOUR API 동기화가 진행 중입니다.");
		}

		OffsetDateTime startedAt = OffsetDateTime.now(clock);
		int changedCount = 0;
		int failedCount = 0;

		try {
			// 시도별로 마지막 페이지까지 조회
			for (var region : regionMapper.findActiveSidoRegions()) {
				try {
					changedCount += synchronizeRegion(region.regionCode());
				} catch (TourApiException exception) {
					failedCount++;
					if (exception.getReason() == TourApiException.Reason.NOT_CONFIGURED
							|| exception.getReason() == TourApiException.Reason.AUTHENTICATION_FAILED) {
						throw exception;
					}
				}
			}

			String status = failedCount == 0 ? "성공" : "부분 성공";
			TourSyncHistoryDTO result = createHistory(startedAt, changedCount, failedCount, status, manager);
			addHistory(result);
			return result;
		} catch (RuntimeException exception) {
			TourSyncHistoryDTO result = createHistory(startedAt, changedCount, Math.max(1, failedCount), "실패", manager);
			addHistory(result);
			throw exception;
		} finally {
			syncing.set(false);
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

	public synchronized List<TourSyncHistoryDTO> getHistory() {
		return List.copyOf(history);
	}

	public OffsetDateTime getLastSyncedAt() {
		return placeMasterMapper.findLastTourApiSyncedAt();
	}

	public boolean isSyncing() {
		return syncing.get();
	}

	private int synchronizeRegion(String regionCode) {
		int page = 1;
		int changedCount = 0;

		while (true) {
			TourApiSearchResult result = tourApiClient.searchArea(regionCode, page, PAGE_SIZE);
			placeCatalogService.rememberTourApiPlaces(result.places());
			changedCount += result.places().size();

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
			OffsetDateTime startedAt,
			int changedCount,
			int failedCount,
			String status,
			String manager
	) {
		String id = "S-" + startedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		String startedAtText = startedAt.atZoneSameInstant(KOREA_ZONE).format(DATE_FORMAT);
		return new TourSyncHistoryDTO(id, startedAtText, changedCount, failedCount, status, manager);
	}

	private synchronized void addHistory(TourSyncHistoryDTO result) {
		history.addFirst(result);
		while (history.size() > HISTORY_SIZE) {
			history.removeLast();
		}
	}
}
