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

import com.noblesi.travelplanner.integration.tourapi.TourApiClient;
import com.noblesi.travelplanner.integration.tourapi.TourApiException;
import com.noblesi.travelplanner.integration.tourapi.TourApiException.Reason;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult.TourApiPlace;
import com.noblesi.travelplanner.service.PlaceSearchService;

@WebMvcTest(PlaceController.class)
@AutoConfigureMockMvc
@Import(PlaceSearchService.class)
class PlaceControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TourApiClient tourApiClient;

	@Test
	void searchesPlacesWithNormalizedResponseContract() throws Exception {
		when(tourApiClient.searchKeyword("한강", "1", 1, 1))
				.thenReturn(new TourApiSearchResult(
						List.of(new TourApiPlace(
								"1001",
								"여의도 한강공원",
								"관광지",
								"서울 영등포구 여의동로 330",
								new BigDecimal("37.5284"),
								new BigDecimal("126.9340"),
								"https://example.com/hanriver.jpg"
						)),
						1,
						1,
						2
				));

		mockMvc.perform(get("/api/places/search")
				.param("keyword", " 한강 ")
				.param("regionCode", "1")
				.param("page", "1")
				.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.places[0].placeProvider").value("TOUR_API"))
				.andExpect(jsonPath("$.data.places[0].externalPlaceId").value("1001"))
				.andExpect(jsonPath("$.data.places[0].placeName").value("여의도 한강공원"))
				.andExpect(jsonPath("$.data.places[0].categoryName").value("관광지"))
				.andExpect(jsonPath("$.data.places[0].latitude").value(37.5284))
				.andExpect(jsonPath("$.data.places[0].longitude").value(126.9340))
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(1))
				.andExpect(jsonPath("$.data.totalCount").value(2))
				.andExpect(jsonPath("$.data.hasNext").value(true));
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
	void mapsTourApiTimeoutToGatewayTimeout() throws Exception {
		when(tourApiClient.searchKeyword("서울", null, 1, 10))
				.thenThrow(new TourApiException(Reason.TIMEOUT, "timed out"));

		mockMvc.perform(get("/api/places/search").param("keyword", "서울"))
				.andExpect(status().isGatewayTimeout())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("TOUR_API_TIMEOUT"));
	}

	@Test
	void mapsMissingTourApiConfigurationToServiceUnavailable() throws Exception {
		when(tourApiClient.searchKeyword("서울", null, 1, 10))
				.thenThrow(new TourApiException(Reason.NOT_CONFIGURED, "missing key"));

		mockMvc.perform(get("/api/places/search").param("keyword", "서울"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("TOUR_API_NOT_CONFIGURED"));
	}
}
