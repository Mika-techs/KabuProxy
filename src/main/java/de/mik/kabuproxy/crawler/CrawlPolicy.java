package de.mik.kabuproxy.crawler;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Timing rules of the crawler: active hours and exponential back-off after network failures.
 */
public final class CrawlPolicy
{
    private static final int MAX_BACKOFF_EXPONENT = 4;

    private CrawlPolicy()
    {
    }

    public static boolean isActive(LocalTime now, LocalTime from, LocalTime to)
    {
        if (from.equals(to))
        {
            return true;
        }
        if (from.isBefore(to))
        {
            return !now.isBefore(from) && now.isBefore(to);
        }
        // window over midnight, e.g. 22:00-06:00
        return !now.isBefore(from) || now.isBefore(to);
    }

    /**
     * interval, 2x, 4x, 8x, 16x (capped) - with 30 min: 30 min … 8 h.
     */
    public static Instant nextAttempt(Instant now, Duration interval, int failCount)
    {
        int exponent = Math.min(Math.max(failCount - 1, 0), MAX_BACKOFF_EXPONENT);
        return now.plus(interval.multipliedBy(1L << exponent));
    }
}
