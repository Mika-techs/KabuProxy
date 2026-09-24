package de.mik.kabuproxy.crawler;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrawlPolicyTest
{
    private static final LocalTime SIX = LocalTime.of(6, 0);
    private static final LocalTime TWENTY_TWO = LocalTime.of(22, 0);

    @Test
    void activeWindow()
    {
        assertTrue(CrawlPolicy.isActive(LocalTime.of(6, 0), SIX, TWENTY_TWO));
        assertTrue(CrawlPolicy.isActive(LocalTime.of(21, 59), SIX, TWENTY_TWO));
        assertFalse(CrawlPolicy.isActive(LocalTime.of(22, 0), SIX, TWENTY_TWO));
        assertFalse(CrawlPolicy.isActive(LocalTime.of(3, 0), SIX, TWENTY_TWO));
        assertTrue(CrawlPolicy.isActive(LocalTime.of(3, 0), TWENTY_TWO, SIX));
        assertTrue(CrawlPolicy.isActive(LocalTime.of(3, 0), SIX, SIX));
    }

    @Test
    void backoffDoublesAndCaps()
    {
        Instant now = Instant.parse("2026-09-24T10:00:00Z");
        Duration interval = Duration.ofMinutes(30);

        assertEquals(now.plus(Duration.ofMinutes(30)), CrawlPolicy.nextAttempt(now, interval, 1));
        assertEquals(now.plus(Duration.ofMinutes(60)), CrawlPolicy.nextAttempt(now, interval, 2));
        assertEquals(now.plus(Duration.ofHours(8)), CrawlPolicy.nextAttempt(now, interval, 5));
        assertEquals(now.plus(Duration.ofHours(8)), CrawlPolicy.nextAttempt(now, interval, 50));
    }
}
