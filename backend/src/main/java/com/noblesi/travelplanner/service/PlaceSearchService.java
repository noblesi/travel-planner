package com.noblesi.travelplanner.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.place.PlaceType;
import com.noblesi.travelplanner.domain.region.Region;
import com.noblesi.travelplanner.dto.place.PlaceSearchItemResponse;
import com.noblesi.travelplanner.dto.place.PlaceSearchResponse;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalClient;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalException;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult.KakaoPlace;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult.TourApiPlace;
import com.noblesi.travelplanner.mapper.RegionMapper;

@Service
public class PlaceSearchService {
	private static final int KAKAO_PROVIDER_PAGE_SIZE = 15;
	private static final int KAKAO_PROVIDER_PAGE_LIMIT = 3;
	private static final Set<String> TRAVEL_INFORMATION_CATEGORIES = Set.of(
			"공항",
			"버스터미널",
			"여객터미널",
			"항구",
			"선착장",
			"휴게소",
			"관광안내소",
			"여행안내소"
	);

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
			var enrichment = tourApiPlaceEnricher.enrichWithComplements(
					result,
					normalizedKeyword,
					regionCode
			);
			List<SearchCandidate> candidates = combinedCandidates(enrichment, normalizedKeyword);
			List<String> categories = candidates.stream()
					.map(SearchCandidate::categoryName)
					.distinct()
					.toList();
			String normalizedCategory = category == null ? "" : category.trim();
			List<SearchCandidate> filteredPlaces = candidates.stream()
					.filter(place -> normalizedCategory.isEmpty()
							|| place.categoryName().equals(normalizedCategory))
					.toList();
			List<SearchCandidate> pagePlaces = pageResult(filteredPlaces, page, size);
			rememberPlaces(pagePlaces);
			return new PlaceSearchResponse(
					pagePlaces.stream().map(SearchCandidate::response).toList(),
					page,
					size,
					filteredPlaces.size(),
					(long) page * size < filteredPlaces.size(),
					categories
			);
		} catch (KakaoLocalException exception) {
			throw mapException(exception);
		}
	}

	private List<SearchCandidate> combinedCandidates(
			TourApiPlaceEnricher.EnrichmentResult enrichment,
			String keyword
	) {
		List<SearchCandidate> candidates = new ArrayList<>();
		enrichment.kakaoResult().places().stream()
				.filter(this::isTravelRelevant)
				.map(SearchCandidate::from)
				.forEach(candidates::add);
		enrichment.complementaryTourPlaces().stream()
				.map(SearchCandidate::from)
				.forEach(candidates::add);
		candidates.sort(Comparator
				.comparingInt((SearchCandidate candidate) -> candidatePriority(candidate, keyword))
				.reversed());
		return List.copyOf(candidates);
	}

	private int candidatePriority(SearchCandidate candidate, String keyword) {
		String normalizedKeyword = normalizeSearchText(keyword);
		String normalizedPlaceName = normalizeSearchText(candidate.response().placeName());
		String normalizedCategory = normalizeSearchText(candidate.categoryName());
		int priority = 0;
		if (!normalizedKeyword.isEmpty() && normalizedPlaceName.equals(normalizedKeyword)) {
			priority += 2_000;
		}
		if (normalizedKeyword.length() >= 2 && normalizedCategory.length() >= 2
				&& (normalizedKeyword.contains(normalizedCategory)
				|| normalizedCategory.contains(normalizedKeyword))) {
			priority += 1_000;
		}
		String imageUrl = candidate.response().imageUrl();
		if (imageUrl != null && !imageUrl.isBlank()) priority += 20;
		if (candidate.kakaoPlace() != null) priority += 10;
		return priority;
	}

	private boolean isTravelRelevant(KakaoPlace place) {
		if (place.placeType() != PlaceType.TOURIST_INFORMATION) {
			return !categoryName(place).endsWith("용품");
		}
		String categoryName = categoryName(place);
		return TRAVEL_INFORMATION_CATEGORIES.contains(categoryName);
	}

	private String normalizeSearchText(String value) {
		if (value == null) return "";
		return Normalizer.normalize(value, Normalizer.Form.NFKC)
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^\\p{L}\\p{N}]", "");
	}

	private void rememberPlaces(List<SearchCandidate> places) {
		placeCatalogService.rememberKakaoPlaces(places.stream()
				.map(SearchCandidate::kakaoPlace)
				.filter(Objects::nonNull)
				.toList());
		placeCatalogService.rememberTourApiPlaces(places.stream()
				.map(SearchCandidate::tourApiPlace)
				.filter(Objects::nonNull)
				.toList());
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

	private List<SearchCandidate> pageResult(List<SearchCandidate> places, int page, int size) {
		int fromIndex = Math.min((page - 1) * size, places.size());
		int toIndex = Math.min(fromIndex + size, places.size());
		return places.subList(fromIndex, toIndex);
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

	private record SearchCandidate(
			PlaceSearchItemResponse response,
			String categoryName,
			KakaoPlace kakaoPlace,
			TourApiPlace tourApiPlace
	) {
		private static SearchCandidate from(KakaoPlace place) {
			return new SearchCandidate(
					PlaceSearchItemResponse.from(place),
					place.categoryName() == null || place.categoryName().isBlank()
							? "기타"
							: place.categoryName(),
					place,
					null
			);
		}

		private static SearchCandidate from(TourApiPlace place) {
			return new SearchCandidate(
					PlaceSearchItemResponse.from(place),
					place.categoryName() == null || place.categoryName().isBlank()
							? "기타"
							: place.categoryName(),
					null,
					place
			);
		}
	}
}
