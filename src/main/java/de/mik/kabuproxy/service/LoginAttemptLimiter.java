package de.mik.kabuproxy.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window limit for credential test logins per app user, so the proxy can't be used to guess digikabu
 * passwords (and doesn't get someone's digikabu account locked).
 */
public class LoginAttemptLimiter
{
    private final int maxAttempts;
    private final Duration window;
    private final Map<Long, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(int maxAttempts, Duration window)
    {
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    /**
     * Records an attempt if the user is still below the limit.
     *
     * @return false when the user has to wait
     */
    public boolean tryAcquire(long userId, Instant now)
    {
        Deque<Instant> recent = attempts.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (recent)
        {
            Instant cutoff = now.minus(window);
            while (!recent.isEmpty() && !recent.peekFirst().isAfter(cutoff))
            {
                recent.pollFirst();
            }
            if (recent.size() >= maxAttempts)
            {
                return false;
            }
            recent.addLast(now);
            return true;
        }
    }
}
