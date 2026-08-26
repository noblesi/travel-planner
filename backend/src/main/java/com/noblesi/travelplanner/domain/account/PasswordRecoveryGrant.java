package com.noblesi.travelplanner.domain.account;

import java.io.Serializable;
import java.time.Instant;

public record PasswordRecoveryGrant(long memberId, Instant expiresAt) implements Serializable {
}
