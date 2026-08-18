package com.noblesi.travelplanner.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TravelTimeConfig {

	public static final ZoneId TRAVEL_ZONE_ID = ZoneId.of("Asia/Seoul");

	@Bean
	Clock travelClock(TravelProperties properties) {
		return properties.fixedClockInstant() == null
				? Clock.system(TRAVEL_ZONE_ID)
				: Clock.fixed(properties.fixedClockInstant(), TRAVEL_ZONE_ID);
	}
}
