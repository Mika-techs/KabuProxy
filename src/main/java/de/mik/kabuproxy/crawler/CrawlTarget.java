package de.mik.kabuproxy.crawler;

import java.time.Instant;

/**
 * Detached snapshot of an account, so the crawler never holds entities across its slow HTTP calls.
 */
public record CrawlTarget(long accountId, String digikabuUsername, String passwordEnc, String className, Instant lastAttemptAt)
{
}
