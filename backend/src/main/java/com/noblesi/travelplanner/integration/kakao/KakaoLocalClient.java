package com.noblesi.travelplanner.integration.kakao;

import org.springframework.stereotype.Component;

@Component
public class KakaoLocalClient {

	private final KakaoLocalHttpClient httpClient;
	private final KakaoLocalResponseParser responseParser;

	public KakaoLocalClient(
			KakaoLocalHttpClient httpClient,
			KakaoLocalResponseParser responseParser
	) {
		this.httpClient = httpClient;
		this.responseParser = responseParser;
	}

	public KakaoLocalSearchResult searchKeyword(String query, int page, int size) {
		String responseBody = httpClient.searchKeyword(query, page, size);
		return responseParser.parse(responseBody, page, size);
	}
}
