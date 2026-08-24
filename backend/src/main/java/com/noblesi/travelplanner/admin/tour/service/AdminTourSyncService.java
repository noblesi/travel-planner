package com.noblesi.travelplanner.admin.tour.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.admin.tour.dto.TourSyncHistoryDTO;
import com.noblesi.travelplanner.admin.tour.dto.TourSyncSummaryDTO;
import com.noblesi.travelplanner.admin.tour.mapper.AdminTourSyncMapper;
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
	private final AdminTourSyncMapper adminTourSyncMapper;
	private final Clock clock;
	private final AtomicBoolean syncing = new AtomicBoolean(false);

	public AdminTourSyncService(
			TourApiClient tourApiClient,
			PlaceCatalogService placeCatalogService,
			PlaceMasterMapper placeMasterMapper,
			RegionMapper regionMapper,
			AdminTourSyncMapper adminTourSyncMapper,
			Clock clock
	) {
		this.tourApiClient = tourApiClient;
		this.placeCatalogService = placeCatalogService;
		this.placeMasterMapper = placeMasterMapper;
		this.regionMapper = regionMapper;
		this.adminTourSyncMapper = adminTourSyncMapper;
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
			try {
				// 시도별로 마지막 페이지까지 조회하고, 개별 지역 장애는 전체 작업을 중단하지 않는다.
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
			} catch (RuntimeException exception) {
				// API 인증 오류처럼 작업 자체가 실패해도 운영자가 원인을 추적할 수 있도록 이력을 남긴다.
				saveHistory(
						startedAt, changedCount, Math.max(1, failedCount), "FAILED", manager,
						exception.getMessage()
				);
				throw exception;
			}

			String status = failedCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS";
			TourSyncHistoryDTO result = saveHistory(
					startedAt, changedCount, failedCount, status, manager,
					failedCount == 0 ? null : failedCount + "개 지역 동기화 실패"
			);
			return result;
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

	public List<TourSyncHistoryDTO> getHistory() {
		// 메모리가 아닌 DB에서 읽으므로 서버 재시작 뒤에도 최근 실행 결과가 유지된다.
		return adminTourSyncMapper.selectRecentHistory(HISTORY_SIZE).stream()
				.map(history -> new TourSyncHistoryDTO(
						"S-" + history.getSyncHistoryId(),
						history.getStartedAt().atZoneSameInstant(KOREA_ZONE).format(DATE_FORMAT),
						history.getSuccessCount(),
						history.getFailCount(),
						toDisplayStatus(history.getProcessStatus()),
						history.getManager()
				))
				.toList();
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

	private TourSyncHistoryDTO saveHistory(
			OffsetDateTime startedAt,
			int changedCount,
			int failedCount,
			String status,
			String manager,
			String errorMessage
	) {
		OffsetDateTime finishedAt = OffsetDateTime.now(clock);
		adminTourSyncMapper.insertHistory(
				manager, startedAt, finishedAt, changedCount, failedCount, status, errorMessage);

		// 저장 직후 화면에 보여줄 결과는 DB와 같은 상태 표현을 사용해 일관성을 유지한다.
		String id = "S-" + startedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		String startedAtText = startedAt.atZoneSameInstant(KOREA_ZONE).format(DATE_FORMAT);
		return new TourSyncHistoryDTO(
				id, startedAtText, changedCount, failedCount, toDisplayStatus(status), manager);
	}

	private String toDisplayStatus(String status) {
		return switch (status) {
			case "SUCCESS" -> "성공";
			case "PARTIAL_SUCCESS" -> "부분 성공";
			default -> "실패";
		};
	}
}
