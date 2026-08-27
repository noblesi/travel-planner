package com.noblesi.travelplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.noblesi.travelplanner.domain.place.PlaceType;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult.KakaoPlace;
import com.noblesi.travelplanner.integration.tourapi.TourApiClient;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult.TourApiPlace;

@ExtendWith(MockitoExtension.class)
class TourApiPlaceEnricherTest {

	@Mock
	private TourApiClient tourApiClient;

	@InjectMocks
	private TourApiPlaceEnricher enricher;

	@Test
	void enrichesMatchingKakaoPlaceWithTourImageAndClassification() {
		KakaoLocalSearchResult kakaoResult = kakaoResult(new KakaoPlace(
				"kakao-1",
				"성산일출봉",
				PlaceType.TOURIST_INFORMATION,
				"장소",
				"제주특별자치도 서귀포시 성산읍",
				new BigDecimal("33.4581"),
				new BigDecimal("126.9425"),
				null
		));
		when(tourApiClient.searchKeyword("성산일출봉", "39", 1, 100))
				.thenReturn(tourResult(new TourApiPlace(
						"tour-1",
						"성산 일출봉",
						PlaceType.ATTRACTION,
						"관광지",
						"제주특별자치도 서귀포시 성산읍",
						new BigDecimal("33.4582"),
						new BigDecimal("126.9426"),
						"https://example.com/seongsan.jpg"
				)));

		KakaoPlace enriched = enricher.enrich(kakaoResult, "성산일출봉", "39")
				.places().getFirst();

		assertThat(enriched.externalPlaceId()).isEqualTo("kakao-1");
		assertThat(enriched.placeType()).isEqualTo(PlaceType.ATTRACTION);
		assertThat(enriched.categoryName()).isEqualTo("관광지");
		assertThat(enriched.imageUrl()).isEqualTo("https://example.com/seongsan.jpg");
	}

	@Test
	void doesNotMergeSameNameWhenCoordinatesAreFarApart() {
		KakaoPlace kakaoPlace = new KakaoPlace(
				"kakao-2",
				"중앙공원",
				PlaceType.ATTRACTION,
				"관광지",
				"서울특별시",
				new BigDecimal("37.5665"),
				new BigDecimal("126.9780"),
				null
		);
		when(tourApiClient.searchKeyword("중앙공원", null, 1, 100))
				.thenReturn(tourResult(new TourApiPlace(
						"tour-2",
						"중앙공원",
						PlaceType.ATTRACTION,
						"관광지",
						"부산광역시",
						new BigDecimal("35.1796"),
						new BigDecimal("129.0756"),
						"https://example.com/busan.jpg"
				)));

		KakaoPlace result = enricher.enrich(kakaoResult(kakaoPlace), "중앙공원", null)
				.places().getFirst();

		assertThat(result).isEqualTo(kakaoPlace);
	}

	@Test
	void returnsUnmatchedTourPlacesAsComplements() {
		KakaoPlace kakaoPlace = new KakaoPlace(
				"kakao-3",
				"제주국제공항",
				PlaceType.TOURIST_INFORMATION,
				"공항",
				"제주특별자치도 제주시",
				new BigDecimal("33.5070"),
				new BigDecimal("126.4927"),
				null
		);
		TourApiPlace tourPlace = new TourApiPlace(
				"tour-3",
				"성산일출봉",
				PlaceType.ATTRACTION,
				"관광지",
				"제주특별자치도 서귀포시 성산읍",
				new BigDecimal("33.4582"),
				new BigDecimal("126.9426"),
				"https://example.com/seongsan.jpg"
		);
		when(tourApiClient.searchKeyword("제주", "39", 1, 100))
				.thenReturn(tourResult(tourPlace));

		var result = enricher.enrichWithComplements(
				kakaoResult(kakaoPlace),
				"제주",
				"39"
		);

		assertThat(result.kakaoResult().places()).containsExactly(kakaoPlace);
		assertThat(result.complementaryTourPlaces()).containsExactly(tourPlace);
	}

	private KakaoLocalSearchResult kakaoResult(KakaoPlace place) {
		return new KakaoLocalSearchResult(List.of(place), 1, 10, 1, false);
	}

	private TourApiSearchResult tourResult(TourApiPlace place) {
		return new TourApiSearchResult(List.of(place), 1, 100, 1);
	}
}
