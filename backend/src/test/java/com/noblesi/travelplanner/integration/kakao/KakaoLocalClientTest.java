package com.noblesi.travelplanner.integration.kakao;

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
import com.noblesi.travelplanner.domain.place.PlaceType;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalException.Reason;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult.KakaoPlace;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.ObjectMapper;

class KakaoLocalClientTest {

	private HttpServer server;
	private final AtomicReference<String> responseBody = new AtomicReference<>();
	private final AtomicReference<String> rawQuery = new AtomicReference<>();
	private final AtomicReference<String> authorization = new AtomicReference<>();
	private final AtomicInteger requestCount = new AtomicInteger();
	private final AtomicInteger responseStatus = new AtomicInteger(200);

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v2/local/search/keyword.json", this::handleRequest);
		server.start();
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	@Test
	void searchesRegionalKeywordAndNormalizesAirport() {
		responseBody.set(successResponse());

		KakaoLocalSearchResult result = client("test-rest-key")
				.searchKeyword("제주특별자치도 공항", 1, 10);

		assertThat(result.page()).isEqualTo(1);
		assertThat(result.size()).isEqualTo(10);
		assertThat(result.totalCount()).isEqualTo(1);
		assertThat(result.hasNext()).isFalse();
		assertThat(result.places()).hasSize(1);
		assertThat(result.places().getFirst())
				.extracting(
						KakaoPlace::externalPlaceId,
						KakaoPlace::placeName,
						KakaoPlace::placeType,
						KakaoPlace::categoryName,
						KakaoPlace::address,
						KakaoPlace::latitude,
						KakaoPlace::longitude
				)
				.containsExactly(
						"10809636",
						"제주국제공항",
						PlaceType.TOURIST_INFORMATION,
						"공항",
						"제주특별자치도 제주시 공항로 2",
						new BigDecimal("33.5070789578184"),
						new BigDecimal("126.492769004244")
				);

		assertThat(authorization).hasValue("KakaoAK test-rest-key");
		assertThat(rawQuery.get())
				.contains("query=")
				.contains("page=1")
				.contains("size=10")
				.contains("sort=accuracy");
	}

	@Test
	void rejectsSearchBeforeRequestWhenRestApiKeyIsMissing() {
		assertThatThrownBy(() -> client(" ").searchKeyword("공항", 1, 10))
				.isInstanceOfSatisfying(KakaoLocalException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(Reason.NOT_CONFIGURED));
		assertThat(requestCount).hasValue(0);
	}

	@Test
	void mapsProviderAuthenticationError() {
		responseStatus.set(401);
		responseBody.set("unauthorized");

		assertThatThrownBy(() -> client("invalid-key").searchKeyword("공항", 1, 10))
				.isInstanceOfSatisfying(KakaoLocalException.class, exception -> {
					assertThat(exception.getReason()).isEqualTo(Reason.AUTHENTICATION_FAILED);
					assertThat(exception.getCause()).isNull();
					assertThat(exception.toString()).doesNotContain("invalid-key");
				});
	}

	@Test
	void mapsMalformedJsonResponse() {
		responseBody.set("not-json");

		assertThatThrownBy(() -> client("test-rest-key").searchKeyword("공항", 1, 10))
				.isInstanceOfSatisfying(KakaoLocalException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(Reason.INVALID_RESPONSE));
	}

	private KakaoLocalClient client(String restApiKey) {
		URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
		ExternalApiProperties properties = new ExternalApiProperties(
				new ExternalApiProperties.TourApi(
						URI.create("https://apis.data.go.kr/B551011/KorService2"),
						"",
						"WithTripTest",
						Duration.ofSeconds(1),
						Duration.ofSeconds(2)
				),
				new ExternalApiProperties.KakaoApi(
						baseUrl,
						restApiKey,
						"",
						Duration.ofSeconds(1),
						Duration.ofSeconds(2)
				)
		);
		return new KakaoLocalClient(
				new KakaoLocalHttpClient(properties),
				new KakaoLocalResponseParser(new ObjectMapper())
		);
	}

	private void handleRequest(HttpExchange exchange) throws IOException {
		requestCount.incrementAndGet();
		rawQuery.set(exchange.getRequestURI().getRawQuery());
		authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
		byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		exchange.sendResponseHeaders(responseStatus.get(), body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private String successResponse() {
		return """
				{
				  "meta": {
				    "total_count": 1,
				    "pageable_count": 1,
				    "is_end": true
				  },
				  "documents": [
				    {
				      "id": "10809636",
				      "place_name": "제주국제공항",
				      "category_name": "교통,수송 > 항공 > 공항",
				      "category_group_code": "",
				      "category_group_name": "",
				      "address_name": "제주특별자치도 제주시 용담이동 2002",
				      "road_address_name": "제주특별자치도 제주시 공항로 2",
				      "x": "126.492769004244",
				      "y": "33.5070789578184"
				    }
				  ]
				}
				""";
	}
}
