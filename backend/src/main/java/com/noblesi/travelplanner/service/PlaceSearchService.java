package com.noblesi.travelplanner.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.region.Region;
import com.noblesi.travelplanner.dto.place.PlaceSearchResponse;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalClient;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalException;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult.KakaoPlace;
import com.noblesi.travelplanner.mapper.RegionMapper;

@Service
public class PlaceSearchService {
	private static final int KAKAO_PROVIDER_PAGE_SIZE = 15;
	private static final int KAKAO_PROVIDER_PAGE_LIMIT = 3;

	private final KakaoLocalClient kakaoLocalClient;
	private final PlaceCatalogService placeCatalogService;
	private final RegionMapper regionMapper;
	private final TourApiPlaceEnricher tourApiPlaceEnricher;

	public PlaceSearchService(
			KakaoLocalClient kakaoLocalClient,
			PlaceCatalogService placeCatalogService,
			RegionMapper regionMapper,
			TourApiPlaceEnricher tourApiPlaceEnricher
	) {
		this.kakaoLocalClient = kakaoLocalClient;
		this.placeCatalogService = placeCatalogService;
		this.regionMapper = regionMapper;
		this.tourApiPlaceEnricher = tourApiPlaceEnricher;
	}

	public PlaceSearchResponse search(
			String keyword,
			String regionCode,
			String category,
			int page,
			int size
	) {
		try {
			String normalizedKeyword = keyword.trim();
			String query = regionalQuery(normalizedKeyword, regionCode);
			var result = collectSearchablePlaces(query);
			var enrichedResult = tourApiPlaceEnricher.enrich(result, normalizedKeyword, regionCode);
			List<String> categories = enrichedResult.places().stream()
					.map(this::categoryName)
					.distinct()
					.toList();
			String normalizedCategory = category == null ? "" : category.trim();
			List<KakaoPlace> filteredPlaces = enrichedResult.places().stream()
					.filter(place -> normalizedCategory.isEmpty()
							|| categoryName(place).equals(normalizedCategory))
					.toList();
			KakaoLocalSearchResult pageResult = pageResult(filteredPlaces, page, size);
			placeCatalogService.rememberKakaoPlaces(pageResult.places());
			return PlaceSearchResponse.from(pageResult, categories);
		} catch (KakaoLocalException exception) {
			throw mapException(exception);
		}
	}

	private KakaoLocalSearchResult collectSearchablePlaces(String query) {
		Map<String, KakaoPlace> placesById = new LinkedHashMap<>();
		for (int providerPage = 1; providerPage <= KAKAO_PROVIDER_PAGE_LIMIT; providerPage++) {
			KakaoLocalSearchResult result = kakaoLocalClient.searchKeyword(
					query,
					providerPage,
					KAKAO_PROVIDER_PAGE_SIZE
			);
			for (KakaoPlace place : result.places()) {
				placesById.putIfAbsent(place.externalPlaceId(), place);
			}
			if (!result.hasNext() || result.places().isEmpty()) break;
		}
		List<KakaoPlace> places = List.copyOf(placesById.values());
		return new KakaoLocalSearchResult(places, 1, places.size(), places.size(), false);
	}

	private KakaoLocalSearchResult pageResult(List<KakaoPlace> places, int page, int size) {
		int fromIndex = Math.min((page - 1) * size, places.size());
		int toIndex = Math.min(fromIndex + size, places.size());
		return new KakaoLocalSearchResult(
				places.subList(fromIndex, toIndex),
				page,
				size,
				places.size(),
				toIndex < places.size()
		);
	}

	private String categoryName(KakaoPlace place) {
		return place.categoryName() == null || place.categoryName().isBlank()
				? "기타"
				: place.categoryName();
	}

	private String regionalQuery(String keyword, String regionCode) {
		if (regionCode == null || regionCode.isBlank()) return keyword;
		Region region = regionMapper.findActiveSidoRegionByCode(regionCode);
		return region == null ? keyword : region.regionName() + " " + keyword;
	}

	private BusinessException mapException(KakaoLocalException exception) {
		return switch (exception.getReason()) {
			case NOT_CONFIGURED -> new BusinessException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"KAKAO_LOCAL_NOT_CONFIGURED",
					"장소 검색 서비스를 사용하기 위한 설정이 필요합니다."
			);
			case AUTHENTICATION_FAILED -> new BusinessException(
					HttpStatus.BAD_GATEWAY,
					"KAKAO_LOCAL_AUTHENTICATION_FAILED",
					"장소 검색 서비스 인증에 실패했습니다."
			);
			case TIMEOUT -> new BusinessException(
					HttpStatus.GATEWAY_TIMEOUT,
					"KAKAO_LOCAL_TIMEOUT",
					"장소 검색 서비스의 응답이 지연되고 있습니다."
			);
			case UNAVAILABLE -> new BusinessException(
					HttpStatus.BAD_GATEWAY,
					"KAKAO_LOCAL_UNAVAILABLE",
					"장소 검색 서비스에 일시적으로 연결할 수 없습니다."
			);
			case INVALID_RESPONSE -> new BusinessException(
					HttpStatus.BAD_GATEWAY,
					"KAKAO_LOCAL_INVALID_RESPONSE",
					"장소 검색 서비스가 올바르지 않은 응답을 반환했습니다."
			);
		};
	}
}
