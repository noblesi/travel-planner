package com.noblesi.travelplanner.integration.tourapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.noblesi.travelplanner.config.ExternalApiProperties;
import com.noblesi.travelplanner.integration.tourapi.TourApiException.Reason;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.ObjectMapper;

class TourApiClientTest {

	private HttpServer server;
	private final AtomicReference<String> responseBody = new AtomicReference<>();
	private final AtomicReference<String> rawQuery = new AtomicReference<>();
	private final AtomicInteger requestCount = new AtomicInteger();

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/B551011/KorService2/searchKeyword2", this::handleRequest);
		server.start();
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	@Test
	void searchesPlacesAndNormalizesTourApiResponse() {
		responseBody.set(successResponse());

		TourApiSearchResult result = client("test-service-key")
				.searchKeyword("한강 공원", "1", 2, 2);

		assertThat(result.page()).isEqualTo(2);
		assertThat(result.size()).isEqualTo(2);
		assertThat(result.totalCount()).isEqualTo(5);
		assertThat(result.places()).hasSize(2);
		assertThat(result.places().getFirst())
				.extracting(
						TourApiSearchResult.TourApiPlace::externalPlaceId,
						TourApiSearchResult.TourApiPlace::placeName,
						TourApiSearchResult.TourApiPlace::categoryName,
						TourApiSearchResult.TourApiPlace::address,
						TourApiSearchResult.TourApiPlace::latitude,
						TourApiSearchResult.TourApiPlace::longitude,
						TourApiSearchResult.TourApiPlace::imageUrl
				)
				.containsExactly(
						"1001",
						"여의도 한강공원",
						"관광지",
						"서울 영등포구 여의동로 330",
						new BigDecimal("37.5284"),
						new BigDecimal("126.9340"),
						"https://example.com/hanriver.jpg"
				);
		assertThat(result.places().get(1).imageUrl())
				.isEqualTo("https://example.com/thumbnail.jpg");

		assertThat(rawQuery.get())
				.contains("serviceKey=test-service-key")
				.contains("MobileOS=ETC")
				.contains("MobileApp=WithTripTest")
				.contains("_type=json")
				.contains("arrange=A")
				.contains("keyword=")
				.contains("pageNo=2")
				.contains("numOfRows=2")
				.contains("areaCode=1");
	}

	@Test
	void returnsEmptyPlacesWhenTourApiHasNoItems() {
		responseBody.set("""
				{
				  "response": {
				    "header": {"resultCode": "0000", "resultMsg": "OK"},
				    "body": {"items": "", "numOfRows": 10, "pageNo": 1, "totalCount": 0}
				  }
				}
				""");

		TourApiSearchResult result = client("test-service-key")
				.searchKeyword("없는 장소", null, 1, 10);

		assertThat(result.places()).isEmpty();
		assertThat(result.totalCount()).isZero();
	}

	@Test
	void mapsProviderAuthenticationError() {
		responseBody.set("""
				{
				  "response": {
				    "header": {
				      "resultCode": "30",
				      "resultMsg": "SERVICE KEY IS NOT REGISTERED ERROR"
				    }
				  }
				}
				""");

		assertThatThrownBy(() -> client("invalid-key").searchKeyword("서울", null, 1, 10))
				.isInstanceOfSatisfying(TourApiException.class, exception -> {
					assertThat(exception.getReason()).isEqualTo(Reason.AUTHENTICATION_FAILED);
					assertThat(exception.getProviderCode()).isEqualTo("30");
				});
	}

	@Test
	void rejectsSearchBeforeRequestWhenServiceKeyIsMissing() {
		assertThatThrownBy(() -> client(" ").searchKeyword("서울", null, 1, 10))
				.isInstanceOfSatisfying(TourApiException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(Reason.NOT_CONFIGURED));
		assertThat(requestCount).hasValue(0);
	}

	@Test
	void mapsMalformedJsonResponse() {
		responseBody.set("not-json");

		assertThatThrownBy(() -> client("test-service-key").searchKeyword("서울", null, 1, 10))
				.isInstanceOfSatisfying(TourApiException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(Reason.INVALID_RESPONSE));
	}

	private TourApiClient client(String serviceKey) {
		URI baseUrl = URI.create(
				"http://127.0.0.1:" + server.getAddress().getPort() + "/B551011/KorService2"
		);
		ExternalApiProperties properties = new ExternalApiProperties(
				new ExternalApiProperties.TourApi(
						baseUrl,
						serviceKey,
						"WithTripTest",
						Duration.ofSeconds(1),
						Duration.ofSeconds(2)
				),
				new ExternalApiProperties.KakaoApi(
						URI.create("https://dapi.kakao.com"),
						"",
						Duration.ofSeconds(1),
						Duration.ofSeconds(2)
				)
		);
		return new TourApiClient(properties, new ObjectMapper());
	}

	private void handleRequest(HttpExchange exchange) throws IOException {
		requestCount.incrementAndGet();
		rawQuery.set(exchange.getRequestURI().getRawQuery());
		byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		exchange.sendResponseHeaders(200, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private String successResponse() {
		return """
				{
				  "response": {
				    "header": {"resultCode": "0000", "resultMsg": "OK"},
				    "body": {
				      "items": {
				        "item": [
				          {
				            "contentid": "1001",
				            "title": "여의도 한강공원",
				            "contenttypeid": "12",
				            "addr1": "서울 영등포구",
				            "addr2": "여의동로 330",
				            "mapy": "37.5284",
				            "mapx": "126.9340",
				            "firstimage": "https://example.com/hanriver.jpg"
				          },
				          {
				            "contentid": "1002",
				            "title": "한강 전망 카페",
				            "contenttypeid": "39",
				            "firstimage": "",
				            "firstimage2": "https://example.com/thumbnail.jpg"
				          }
				        ]
				      },
				      "numOfRows": 2,
				      "pageNo": 2,
				      "totalCount": 5
				    }
				  }
				}
				""";
	}
}
