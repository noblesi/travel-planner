package com.noblesi.travelplanner.integration.tourapi;

import org.springframework.stereotype.Component;

@Component
public class TourApiClient {

	private final TourApiHttpClient httpClient;
	private final TourApiResponseParser responseParser;

	public TourApiClient(
			TourApiHttpClient httpClient,
			TourApiResponseParser responseParser
	) {
		this.httpClient = httpClient;
		this.responseParser = responseParser;
	}

	public TourApiSearchResult searchKeyword(
			String keyword,
			String regionCode,
			int page,
			int size
	) {
		String responseBody = httpClient.searchKeyword(keyword, regionCode, page, size);
		return responseParser.parse(responseBody, page, size);
	}
}
