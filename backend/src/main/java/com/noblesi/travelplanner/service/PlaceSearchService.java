package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.dto.place.PlaceSearchResponse;
import com.noblesi.travelplanner.integration.tourapi.TourApiClient;
import com.noblesi.travelplanner.integration.tourapi.TourApiException;

@Service
public class PlaceSearchService {

	private final TourApiClient tourApiClient;
	private final PlaceCatalogService placeCatalogService;

	public PlaceSearchService(TourApiClient tourApiClient, PlaceCatalogService placeCatalogService) {
		this.tourApiClient = tourApiClient;
		this.placeCatalogService = placeCatalogService;
	}

	public PlaceSearchResponse search(String keyword, String regionCode, int page, int size) {
		try {
			var result = tourApiClient.searchKeyword(keyword.trim(), regionCode, page, size);
			placeCatalogService.rememberTourApiPlaces(result.places());
			return PlaceSearchResponse.from(result);
		} catch (TourApiException exception) {
			throw mapException(exception);
		}
	}

	private BusinessException mapException(TourApiException exception) {
		return switch (exception.getReason()) {
			case NOT_CONFIGURED -> new BusinessException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"TOUR_API_NOT_CONFIGURED",
					"장소 검색 서비스를 사용하기 위한 설정이 필요합니다."
			);
			case AUTHENTICATION_FAILED -> new BusinessException(
					HttpStatus.BAD_GATEWAY,
					"TOUR_API_AUTHENTICATION_FAILED",
					"장소 검색 서비스 인증에 실패했습니다."
			);
			case TIMEOUT -> new BusinessException(
					HttpStatus.GATEWAY_TIMEOUT,
					"TOUR_API_TIMEOUT",
					"장소 검색 서비스의 응답이 지연되고 있습니다."
			);
			case UNAVAILABLE -> new BusinessException(
					HttpStatus.BAD_GATEWAY,
					"TOUR_API_UNAVAILABLE",
					"장소 검색 서비스에 일시적으로 연결할 수 없습니다."
			);
			case INVALID_RESPONSE -> new BusinessException(
					HttpStatus.BAD_GATEWAY,
					"TOUR_API_INVALID_RESPONSE",
					"장소 검색 서비스가 올바르지 않은 응답을 반환했습니다."
			);
		};
	}
}
