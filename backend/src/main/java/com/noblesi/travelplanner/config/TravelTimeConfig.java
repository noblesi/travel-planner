package com.noblesi.travelplanner.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TravelTimeConfig {

	public static final ZoneId TRAVEL_ZONE_ID = ZoneId.of("Asia/Seoul");

	@Bean
	Clock travelClock(@Value("${app.travel.fixed-clock-instant:}") String fixedClockInstant) {
		return fixedClockInstant.isBlank()
				? Clock.system(TRAVEL_ZONE_ID)
				: Clock.fixed(Instant.parse(fixedClockInstant), TRAVEL_ZONE_ID);
	}
}
