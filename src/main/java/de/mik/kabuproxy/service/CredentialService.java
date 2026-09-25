package de.mik.kabuproxy.service;

import de.mik.kabuproxy.crawler.CrawlService;
import de.mik.kabuproxy.crypto.CredentialCipher;
import de.mik.kabuproxy.digikabu.DigikabuException;
import de.mik.kabuproxy.digikabu.parser.ParsedHeader;
import org.apache.logging.log4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;

/**
 * Links digikabu credentials to an app user, either by an admin or by the user themselves. The credentials are
 * verified with one real login first, so typos never end up in the crawler.
 */
@ApplicationScoped
public class CredentialService
{
    private static final int MAX_SELF_ATTEMPTS = 5;
    private static final Duration SELF_ATTEMPT_WINDOW = Duration.ofMinutes(15);

    private final LoginAttemptLimiter selfLimiter = new LoginAttemptLimiter(MAX_SELF_ATTEMPTS, SELF_ATTEMPT_WINDOW);

    @Inject private Logger logger;
    @Inject private CredentialCipher cipher;
    @Inject private AccountService accountService;
    @Inject private UserService userService;
    @Inject private CrawlService crawlService;

    public boolean isReady()
    {
        return cipher.isConfigured();
    }

    /**
     * Admin links an account; activates a pending user.
     */
    public LinkResult linkByAdmin(long userId, String digikabuUsername, String password)
    {
        return link(userId, digikabuUsername, password, true);
    }

    /**
     * A user links their own account. Rate limited; does not activate a pending user (an admin still approves).
     */
    public LinkResult linkOwn(long userId, String digikabuUsername, String password)
    {
        if (isBlank(digikabuUsername) || isBlank(password))
        {
            return LinkResult.of(LinkResult.Outcome.INCOMPLETE);
        }
        if (!selfLimiter.tryAcquire(userId, Instant.now()))
        {
            logger.warn("user {}: too many digikabu test logins", userId);
            return LinkResult.of(LinkResult.Outcome.RATE_LIMITED);
        }
        return link(userId, digikabuUsername, password, false);
    }

    private LinkResult link(long userId, String digikabuUsername, String password, boolean activate)
    {
        if (isBlank(digikabuUsername) || isBlank(password))
        {
            return LinkResult.of(LinkResult.Outcome.INCOMPLETE);
        }
        if (!cipher.isConfigured())
        {
            return LinkResult.of(LinkResult.Outcome.NOT_CONFIGURED);
        }
        String username = digikabuUsername.trim();
        ParsedHeader header;
        try
        {
            header = crawlService.testLogin(username, password);
        }
        catch (DigikabuException.AuthFailed e)
        {
            logger.info("user {}: digikabu rejected the credentials", userId);
            return LinkResult.of(LinkResult.Outcome.REJECTED);
        }
        catch (DigikabuException e)
        {
            logger.warn("user {}: test login failed: {}", userId, e.getMessage());
            return new LinkResult(LinkResult.Outcome.FAILED, null, e.getMessage());
        }

        long accountId = accountService.saveCredentials(userId, username, password, header, activate);
        if (userService.isActive(userId))
        {
            crawlService.submit(accountId);
        }
        return new LinkResult(LinkResult.Outcome.SAVED, header, null);
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    /**
     * @param header set when saved
     * @param error  technical detail when the test login failed for other reasons than wrong credentials
     */
    public record LinkResult(Outcome outcome, ParsedHeader header, String error)
    {
        static LinkResult of(Outcome outcome)
        {
            return new LinkResult(outcome, null, null);
        }

        public enum Outcome
        {
            SAVED, INCOMPLETE, NOT_CONFIGURED, RATE_LIMITED, REJECTED, FAILED
        }
    }
}
