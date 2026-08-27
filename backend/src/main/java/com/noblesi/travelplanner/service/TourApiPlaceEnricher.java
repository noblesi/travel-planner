package com.noblesi.travelplanner.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.domain.place.PlaceType;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult.KakaoPlace;
import com.noblesi.travelplanner.integration.tourapi.TourApiClient;
import com.noblesi.travelplanner.integration.tourapi.TourApiException;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult.TourApiPlace;

@Component
public class TourApiPlaceEnricher {

	private static final Logger log = LoggerFactory.getLogger(TourApiPlaceEnricher.class);
	private static final int ENRICHMENT_RESULT_SIZE = 100;
	private static final double EXACT_NAME_DISTANCE_LIMIT_METERS = 1_000;
	private static final double PARTIAL_NAME_DISTANCE_LIMIT_METERS = 300;
	private static final double EARTH_RADIUS_METERS = 6_371_000;

	private final TourApiClient tourApiClient;

	public TourApiPlaceEnricher(TourApiClient tourApiClient) {
		this.tourApiClient = tourApiClient;
	}

	public KakaoLocalSearchResult enrich(
			KakaoLocalSearchResult kakaoResult,
			String keyword,
			String regionCode
	) {
		return enrichWithComplements(kakaoResult, keyword, regionCode).kakaoResult();
	}

	public EnrichmentResult enrichWithComplements(
			KakaoLocalSearchResult kakaoResult,
			String keyword,
			String regionCode
	) {
		try {
			var tourResult = tourApiClient.searchKeyword(
					keyword,
					regionCode,
					1,
					ENRICHMENT_RESULT_SIZE
			);
			if (tourResult.places().isEmpty()) {
				return new EnrichmentResult(kakaoResult, List.of());
			}

			Set<String> matchedTourPlaceIds = new HashSet<>();
			List<KakaoPlace> enrichedPlaces = kakaoResult.places().stream()
					.map(place -> enrichPlace(place, tourResult.places(), matchedTourPlaceIds))
					.toList();
			KakaoLocalSearchResult enrichedKakaoResult = new KakaoLocalSearchResult(
					enrichedPlaces,
					kakaoResult.page(),
					kakaoResult.size(),
					kakaoResult.totalCount(),
					kakaoResult.hasNext()
			);
			List<TourApiPlace> complementaryTourPlaces = tourResult.places().stream()
					.filter(place -> !matchedTourPlaceIds.contains(place.externalPlaceId()))
					.toList();
			return new EnrichmentResult(enrichedKakaoResult, complementaryTourPlaces);
		} catch (TourApiException exception) {
			if (exception.getReason() == TourApiException.Reason.NOT_CONFIGURED) {
				log.debug("TourAPI place enrichment is not configured");
			} else {
				log.warn("TourAPI place enrichment skipped. reason={}", exception.getReason());
			}
			return new EnrichmentResult(kakaoResult, List.of());
		}
	}

	private KakaoPlace enrichPlace(
			KakaoPlace kakaoPlace,
			List<TourApiPlace> tourPlaces,
			Set<String> matchedTourPlaceIds
	) {
		TourApiPlace match = tourPlaces.stream()
				.map(tourPlace -> candidate(kakaoPlace, tourPlace))
				.filter(candidate -> candidate.nameScore() > 0)
				.min(Comparator
						.comparingInt(MatchCandidate::nameScore).reversed()
						.thenComparingDouble(MatchCandidate::distanceMeters))
				.map(MatchCandidate::place)
				.orElse(null);
		if (match == null) return kakaoPlace;
		matchedTourPlaceIds.add(match.externalPlaceId());

		PlaceType placeType = kakaoPlace.placeType() == PlaceType.TOURIST_INFORMATION
				? match.placeType()
				: kakaoPlace.placeType();
		String categoryName = isGenericCategory(kakaoPlace.categoryName())
				? match.categoryName()
				: kakaoPlace.categoryName();
		return new KakaoPlace(
				kakaoPlace.externalPlaceId(),
				kakaoPlace.placeName(),
				placeType,
				categoryName,
				kakaoPlace.address(),
				kakaoPlace.latitude(),
				kakaoPlace.longitude(),
				match.imageUrl()
		);
	}

	private MatchCandidate candidate(KakaoPlace kakaoPlace, TourApiPlace tourPlace) {
		String kakaoName = normalizeName(kakaoPlace.placeName());
		String tourName = normalizeName(tourPlace.placeName());
		int nameScore = nameScore(kakaoName, tourName);
		if (nameScore == 0) return new MatchCandidate(tourPlace, 0, Double.MAX_VALUE);

		double distance = distanceMeters(kakaoPlace, tourPlace);
		if (distance == Double.MAX_VALUE) {
			return new MatchCandidate(tourPlace, nameScore == 2 ? nameScore : 0, distance);
		}
		double limit = nameScore == 2
				? EXACT_NAME_DISTANCE_LIMIT_METERS
				: PARTIAL_NAME_DISTANCE_LIMIT_METERS;
		return new MatchCandidate(tourPlace, distance <= limit ? nameScore : 0, distance);
	}

	private int nameScore(String kakaoName, String tourName) {
		if (kakaoName.isEmpty() || tourName.isEmpty()) return 0;
		if (kakaoName.equals(tourName)) return 2;
		if (Math.min(kakaoName.length(), tourName.length()) >= 4
				&& (kakaoName.contains(tourName) || tourName.contains(kakaoName))) {
			return 1;
		}
		return 0;
	}

	private String normalizeName(String name) {
		if (name == null) return "";
		return Normalizer.normalize(name, Normalizer.Form.NFKC)
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^\\p{L}\\p{N}]", "");
	}

	private boolean isGenericCategory(String categoryName) {
		return categoryName == null || categoryName.isBlank() || "장소".equals(categoryName);
	}

	private double distanceMeters(KakaoPlace kakaoPlace, TourApiPlace tourPlace) {
		if (hasMissingCoordinate(
				kakaoPlace.latitude(),
				kakaoPlace.longitude(),
				tourPlace.latitude(),
				tourPlace.longitude()
		)) {
			return Double.MAX_VALUE;
		}

		double latitude1 = Math.toRadians(kakaoPlace.latitude().doubleValue());
		double latitude2 = Math.toRadians(tourPlace.latitude().doubleValue());
		double latitudeDelta = latitude2 - latitude1;
		double longitudeDelta = Math.toRadians(
				tourPlace.longitude().doubleValue() - kakaoPlace.longitude().doubleValue()
		);
		double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
				+ Math.cos(latitude1) * Math.cos(latitude2)
				* Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
		return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
	}

	private boolean hasMissingCoordinate(BigDecimal... coordinates) {
		for (BigDecimal coordinate : coordinates) {
			if (coordinate == null) return true;
		}
		return false;
	}

	private record MatchCandidate(TourApiPlace place, int nameScore, double distanceMeters) {
	}

	public record EnrichmentResult(
			KakaoLocalSearchResult kakaoResult,
			List<TourApiPlace> complementaryTourPlaces
	) {
		public EnrichmentResult {
			complementaryTourPlaces = List.copyOf(complementaryTourPlaces);
		}
	}
}
