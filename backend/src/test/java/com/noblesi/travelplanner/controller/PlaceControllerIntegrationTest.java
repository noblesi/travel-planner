package com.noblesi.travelplanner.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.noblesi.travelplanner.domain.place.PlaceType;
import com.noblesi.travelplanner.domain.region.Region;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalClient;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalException;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalException.Reason;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult.KakaoPlace;
import com.noblesi.travelplanner.integration.tourapi.TourApiClient;
import com.noblesi.travelplanner.integration.tourapi.TourApiException;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult.TourApiPlace;
import com.noblesi.travelplanner.mapper.RegionMapper;
import com.noblesi.travelplanner.service.PlaceCatalogService;
import com.noblesi.travelplanner.service.PlaceSearchService;
import com.noblesi.travelplanner.service.TourApiPlaceEnricher;

@WebMvcTest(PlaceController.class)
@AutoConfigureMockMvc
@Import({PlaceSearchService.class, TourApiPlaceEnricher.class})
class PlaceControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private KakaoLocalClient kakaoLocalClient;

	@MockitoBean
	private TourApiClient tourApiClient;

	@MockitoBean
	private PlaceCatalogService placeCatalogService;

	@MockitoBean
	private RegionMapper regionMapper;

	@Test
	void searchesPlacesWithNormalizedResponseContract() throws Exception {
		when(regionMapper.findActiveSidoRegionByCode("1"))
				.thenReturn(new Region("1", "서울특별시", "SIDO", null));
		when(kakaoLocalClient.searchKeyword("서울특별시 한강", 1, 15))
				.thenReturn(new KakaoLocalSearchResult(
						List.of(new KakaoPlace(
								"1001",
								"여의도 한강공원",
								PlaceType.ATTRACTION,
								"관광지",
								"서울 영등포구 여의동로 330",
								new BigDecimal("37.5284"),
								new BigDecimal("126.9340"),
								null
						)),
						1,
						15,
						1,
						false
				));
		when(tourApiClient.searchKeyword("한강", "1", 1, 100))
				.thenReturn(new TourApiSearchResult(
						List.of(new TourApiPlace(
								"tour-1001",
								"여의도 한강공원",
								PlaceType.ATTRACTION,
								"관광지",
								"서울 영등포구 여의동로 330",
								new BigDecimal("37.5285"),
								new BigDecimal("126.9341"),
								"https://example.com/hanriver.jpg"
						)),
						1,
						100,
						1
				));

		mockMvc.perform(get("/api/places/search")
				.param("keyword", " 한강 ")
				.param("regionCode", "1")
				.param("page", "1")
				.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.places[0].placeProvider").value("KAKAO"))
				.andExpect(jsonPath("$.data.places[0].externalPlaceId").value("1001"))
				.andExpect(jsonPath("$.data.places[0].placeName").value("여의도 한강공원"))
				.andExpect(jsonPath("$.data.places[0].placeType").value("ATTRACTION"))
				.andExpect(jsonPath("$.data.places[0].categoryName").value("관광지"))
				.andExpect(jsonPath("$.data.places[0].latitude").value(37.5284))
				.andExpect(jsonPath("$.data.places[0].longitude").value(126.9340))
				.andExpect(jsonPath("$.data.places[0].imageUrl")
						.value("https://example.com/hanriver.jpg"))
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(1))
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.hasNext").value(false))
				.andExpect(jsonPath("$.data.categories[0]").value("관광지"));
	}

	@Test
	void filtersAcrossAllSearchableKakaoCategoriesBeforePaging() throws Exception {
		when(regionMapper.findActiveSidoRegionByCode("39"))
				.thenReturn(new Region("39", "제주특별자치도", "SIDO", null));
		when(kakaoLocalClient.searchKeyword("제주특별자치도 공항", 1, 15))
				.thenReturn(new KakaoLocalSearchResult(
						List.of(
								kakaoPlace("1", "제주국제공항", "공항"),
								kakaoPlace("2", "공항식당", "음식점")
						),
						1,
						15,
						4,
						true
				));
		when(kakaoLocalClient.searchKeyword("제주특별자치도 공항", 2, 15))
				.thenReturn(new KakaoLocalSearchResult(
						List.of(
								kakaoPlace("3", "제주공항주차장", "주차장"),
								kakaoPlace("4", "공항라운지", "공항라운지")
						),
						2,
						15,
						4,
						false
				));
		when(tourApiClient.searchKeyword("공항", "39", 1, 100))
				.thenReturn(new TourApiSearchResult(List.of(), 1, 100, 0));

		mockMvc.perform(get("/api/places/search")
				.param("keyword", "공항")
				.param("regionCode", "39")
				.param("category", "음식점")
				.param("page", "1")
				.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.places[0].placeName").value("공항식당"))
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.hasNext").value(false))
				.andExpect(jsonPath("$.data.categories.length()").value(2))
				.andExpect(jsonPath("$.data.categories[0]").value("공항"))
				.andExpect(jsonPath("$.data.categories[1]").value("음식점"));
	}

	@Test
	void excludesAncillaryKakaoFacilitiesAndPrioritizesTourApiPlacesWithImages() throws Exception {
		when(regionMapper.findActiveSidoRegionByCode("39"))
				.thenReturn(new Region("39", "제주특별자치도", "SIDO", null));
		when(kakaoLocalClient.searchKeyword("제주특별자치도 제주", 1, 15))
				.thenReturn(new KakaoLocalSearchResult(
						List.of(
								kakaoPlace("airport-1", "제주국제공항", "공항"),
								kakaoPlace("parking-1", "제주공항 주차장", "주차장")
						),
						1,
						15,
						2,
						false
				));
		when(tourApiClient.searchKeyword("제주", "39", 1, 100))
				.thenReturn(new TourApiSearchResult(
						List.of(new TourApiPlace(
								"tour-2001",
								"성산일출봉",
								PlaceType.ATTRACTION,
								"관광지",
								"제주특별자치도 서귀포시 성산읍",
								new BigDecimal("33.4582"),
								new BigDecimal("126.9426"),
								"https://example.com/seongsan.jpg"
						)),
						1,
						100,
						1
				));

		mockMvc.perform(get("/api/places/search")
				.param("keyword", "제주")
				.param("regionCode", "39"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.places.length()").value(2))
				.andExpect(jsonPath("$.data.places[0].placeProvider").value("TOUR_API"))
				.andExpect(jsonPath("$.data.places[0].placeName").value("성산일출봉"))
				.andExpect(jsonPath("$.data.places[0].imageUrl")
						.value("https://example.com/seongsan.jpg"))
				.andExpect(jsonPath("$.data.places[1].placeName").value("제주국제공항"))
				.andExpect(jsonPath("$.data.categories[0]").value("관광지"))
				.andExpect(jsonPath("$.data.categories[1]").value("공항"));
	}

	@Test
	void keepsExactTravelCategoryAheadOfTourApiImageComplements() throws Exception {
		when(regionMapper.findActiveSidoRegionByCode("39"))
				.thenReturn(new Region("39", "제주특별자치도", "SIDO", null));
		when(kakaoLocalClient.searchKeyword("제주특별자치도 공항", 1, 15))
				.thenReturn(new KakaoLocalSearchResult(
						List.of(
								kakaoPlace("airport-2", "제주국제공항", "공항"),
								new KakaoPlace(
										"outdoor-1",
										"오쉐어 공항본점",
										PlaceType.LEISURE_SPORTS,
										"아웃도어용품",
										"제주특별자치도 제주시",
										new BigDecimal("33.5060"),
										new BigDecimal("126.4910"),
										null
								)
						),
						1,
						15,
						2,
						false
				));
		when(tourApiClient.searchKeyword("공항", "39", 1, 100))
				.thenReturn(new TourApiSearchResult(
						List.of(new TourApiPlace(
								"tour-restaurant-1",
								"귤품은흑돼지 제주공항점",
								PlaceType.RESTAURANT,
								"음식점",
								"제주특별자치도 제주시",
								new BigDecimal("33.5000"),
								new BigDecimal("126.5000"),
								"https://example.com/restaurant.jpg"
						)),
						1,
						100,
						1
				));

		mockMvc.perform(get("/api/places/search")
				.param("keyword", "공항")
				.param("regionCode", "39"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalCount").value(2))
				.andExpect(jsonPath("$.data.places[0].placeName").value("제주국제공항"))
				.andExpect(jsonPath("$.data.places[1].placeProvider").value("TOUR_API"))
				.andExpect(jsonPath("$.data.places[1].imageUrl")
						.value("https://example.com/restaurant.jpg"));
	}

	@Test
	void rejectsInvalidSearchParameters() throws Exception {
		mockMvc.perform(get("/api/places/search")
				.param("keyword", " ")
				.param("regionCode", "SEOUL")
				.param("page", "0")
				.param("size", "21"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));
	}

	@Test
	void rejectsNonNumericPageParameter() throws Exception {
		mockMvc.perform(get("/api/places/search")
				.param("keyword", "서울")
				.param("page", "first"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));
	}

	@Test
	void mapsKakaoTimeoutToGatewayTimeout() throws Exception {
		when(kakaoLocalClient.searchKeyword("서울", 1, 15))
				.thenThrow(new KakaoLocalException(Reason.TIMEOUT, "timed out"));

		mockMvc.perform(get("/api/places/search").param("keyword", "서울"))
				.andExpect(status().isGatewayTimeout())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("KAKAO_LOCAL_TIMEOUT"));
	}

	@Test
	void mapsMissingKakaoConfigurationToServiceUnavailable() throws Exception {
		when(kakaoLocalClient.searchKeyword("서울", 1, 15))
				.thenThrow(new KakaoLocalException(Reason.NOT_CONFIGURED, "missing key"));

		mockMvc.perform(get("/api/places/search").param("keyword", "서울"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("KAKAO_LOCAL_NOT_CONFIGURED"));
	}

	@Test
	void returnsKakaoResultsWhenTourApiEnrichmentFails() throws Exception {
		when(kakaoLocalClient.searchKeyword("제주특별자치도 공항", 1, 15))
				.thenReturn(new KakaoLocalSearchResult(
						List.of(new KakaoPlace(
								"10809636",
								"제주국제공항",
								PlaceType.TOURIST_INFORMATION,
								"공항",
								"제주특별자치도 제주시 공항로 2",
								new BigDecimal("33.5070789"),
								new BigDecimal("126.4927690"),
								null
						)),
						1,
						10,
						1,
						false
				));
		when(regionMapper.findActiveSidoRegionByCode("39"))
				.thenReturn(new Region("39", "제주특별자치도", "SIDO", null));
		when(tourApiClient.searchKeyword("공항", "39", 1, 100))
				.thenThrow(new TourApiException(TourApiException.Reason.TIMEOUT, "timed out"));

		mockMvc.perform(get("/api/places/search")
				.param("keyword", "공항")
				.param("regionCode", "39"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.places[0].placeProvider").value("KAKAO"))
				.andExpect(jsonPath("$.data.places[0].placeName").value("제주국제공항"));
	}

	private KakaoPlace kakaoPlace(String id, String name, String categoryName) {
		PlaceType placeType = "음식점".equals(categoryName)
				? PlaceType.RESTAURANT
				: PlaceType.TOURIST_INFORMATION;
		return new KakaoPlace(
				id,
				name,
				placeType,
				categoryName,
				"제주특별자치도 제주시",
				new BigDecimal("33.5070"),
				new BigDecimal("126.4927"),
				null
		);
	}
}
