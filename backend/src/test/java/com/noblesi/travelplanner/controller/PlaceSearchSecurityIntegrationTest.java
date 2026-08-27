package com.noblesi.travelplanner.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.noblesi.travelplanner.integration.kakao.KakaoLocalClient;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult;
import com.noblesi.travelplanner.integration.tourapi.TourApiClient;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult;
import com.noblesi.travelplanner.mapper.RegionMapper;
import com.noblesi.travelplanner.service.PlaceCatalogService;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_place_search_security;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PlaceSearchSecurityIntegrationTest {

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
	void rejectsAnonymousSearchBeforeCallingExternalPlaceApi() throws Exception {
		mockMvc.perform(get("/api/places/search").param("keyword", "서울"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("CURRENT_MEMBER_NOT_AVAILABLE"));

		verifyNoInteractions(kakaoLocalClient, tourApiClient, placeCatalogService, regionMapper);
	}

	@Test
	void allowsAuthenticatedMemberSearch() throws Exception {
		when(kakaoLocalClient.searchKeyword("서울", 1, 15))
				.thenReturn(new KakaoLocalSearchResult(List.of(), 1, 15, 0, false));
		when(tourApiClient.searchKeyword("서울", null, 1, 100))
				.thenReturn(new TourApiSearchResult(List.of(), 1, 100, 0));

		mockMvc.perform(get("/api/places/search")
					.with(user("member@example.com").roles("MEMBER"))
					.param("keyword", "서울"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.places").isEmpty());
	}
}
