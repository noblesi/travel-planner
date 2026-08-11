package com.noblesi.travelplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalImageUrlPolicyTest {

	private final ExternalImageUrlPolicy policy = new ExternalImageUrlPolicy();

	@Test
	void acceptsHttpAndHttpsUrls() {
		assertThat(policy.sanitize(" https://images.example/place.jpg "))
				.isEqualTo("https://images.example/place.jpg");
		assertThat(policy.sanitize("http://images.example/place.jpg"))
				.isEqualTo("http://images.example/place.jpg");
	}

	@Test
	void rejectsUnsafeOrMalformedUrls() {
		assertThat(policy.sanitize("javascript:alert(1)")).isNull();
		assertThat(policy.sanitize("data:image/png;base64,AAAA")).isNull();
		assertThat(policy.sanitize("file:///tmp/place.jpg")).isNull();
		assertThat(policy.sanitize("https://user:password@images.example/place.jpg")).isNull();
		assertThat(policy.sanitize("https:///missing-host.jpg")).isNull();
		assertThat(policy.sanitize("not a url")).isNull();
	}
}
