package de.mik.kabuproxy.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTest
{
    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    @Test
    void blocksAfterLimitWithinWindow()
    {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(2, Duration.ofMinutes(15));

        assertTrue(limiter.tryAcquire(1, NOW));
        assertTrue(limiter.tryAcquire(1, NOW.plusSeconds(60)));
        assertFalse(limiter.tryAcquire(1, NOW.plusSeconds(120)));
        // other users are independent
        assertTrue(limiter.tryAcquire(2, NOW.plusSeconds(120)));
    }

    @Test
    void oldAttemptsExpire()
    {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(2, Duration.ofMinutes(15));
        limiter.tryAcquire(1, NOW);
        limiter.tryAcquire(1, NOW.plusSeconds(60));

        assertTrue(limiter.tryAcquire(1, NOW.plus(Duration.ofMinutes(15))));
        assertFalse(limiter.tryAcquire(1, NOW.plus(Duration.ofMinutes(15)).plusSeconds(1)));
    }
}
