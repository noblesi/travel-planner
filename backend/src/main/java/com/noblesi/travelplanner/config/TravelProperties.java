package com.noblesi.travelplanner.config;

import java.time.Instant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.travel")
public record TravelProperties(Instant fixedClockInstant) {
}
