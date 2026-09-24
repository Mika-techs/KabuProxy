package de.mik.kabuproxy.crawler;

import de.mik.kabuproxy.config.KabuConfig;
import de.mik.kabuproxy.crypto.CredentialCipher;
import de.mik.kabuproxy.digikabu.DigikabuException;
import de.mik.kabuproxy.digikabu.DigikabuSession;
import de.mik.kabuproxy.digikabu.parser.AbsenceParser;
import de.mik.kabuproxy.digikabu.parser.ExamPlanParser;
import de.mik.kabuproxy.digikabu.parser.HeaderParser;
import de.mik.kabuproxy.digikabu.parser.ParsedHeader;
import de.mik.kabuproxy.digikabu.parser.ParsedWeek;
import de.mik.kabuproxy.digikabu.parser.TimetableParser;
import de.mik.kabuproxy.persistence.entities.CrawlStatus;
import org.apache.logging.log4j.Logger;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Crawls digikabu. Everything runs strictly sequentially behind one lock: digikabu keeps the displayed week in the
 * server session, and we want to stay a polite, low-volume client.
 */
@ApplicationScoped
public class CrawlService
{
    private static final int WEEK = 7;

    private final ReentrantLock lock = new ReentrantLock();

    @Inject private Logger logger;
    @Inject private KabuConfig config;
    @Inject private CredentialCipher cipher;
    @Inject private CrawlStore store;

    @Resource
    private ManagedExecutorService executor;

    /**
     * One scheduled pass over all crawlable accounts. Class-level data (timetable, exam plan) is fetched once per class.
     */
    public void runCycle()
    {
        lock.lock();
        try
        {
            List<CrawlTarget> targets = store.loadCrawlable(Instant.now());
            logger.info("crawl cycle: {} account(s)", targets.size());
            Set<String> classesDone = new HashSet<>();
            for (CrawlTarget target : targets)
            {
                crawl(target, classesDone);
            }
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * "Refresh now" from the UI, rate limited per account.
     */
    public RefreshResult requestRefresh(long userId)
    {
        Optional<CrawlTarget> target = store.loadByUser(userId);
        if (target.isEmpty())
        {
            return RefreshResult.NO_ACCOUNT;
        }
        Instant last = target.get().lastAttemptAt();
        if (last != null && last.plus(config.getRefreshCooldown()).isAfter(Instant.now()))
        {
            return RefreshResult.COOLDOWN;
        }
        if (lock.isLocked())
        {
            return RefreshResult.BUSY;
        }
        submit(target.get().accountId());
        return RefreshResult.STARTED;
    }

    /**
     * Admin-triggered crawl, ignores the cooldown and back-off.
     */
    public void submit(long accountId)
    {
        executor.execute(() -> crawlNow(accountId));
    }

    /**
     * Blocking single-account crawl.
     */
    public void crawlNow(long accountId)
    {
        lock.lock();
        try
        {
            store.loadByAccount(accountId).ifPresent(target -> crawl(target, new HashSet<>()));
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Checks credentials without storing anything; used by the admin before saving an account.
     */
    public ParsedHeader testLogin(String username, String password) throws DigikabuException
    {
        try (DigikabuSession session = new DigikabuSession(config.getDigikabuBaseUrl(), config.getRequestDelayMillis()))
        {
            session.login(username, password);
            return HeaderParser.parse(session.fetchMainPage());
        }
    }

    private void crawl(CrawlTarget target, Set<String> classesDone)
    {
        Instant started = Instant.now();
        long accountId = target.accountId();
        store.markAttempt(accountId, started);

        String password;
        try
        {
            password = cipher.decrypt(target.passwordEnc(), target.digikabuUsername());
        }
        catch (IllegalStateException e)
        {
            logger.error("account {}: {}", accountId, e.getMessage());
            store.markFailure(accountId, CrawlStatus.UNAVAILABLE, "Passwort nicht entschlüsselbar (KABU_CRED_KEY geändert?)", null);
            return;
        }

        DigikabuSession session = new DigikabuSession(config.getDigikabuBaseUrl(), config.getRequestDelayMillis());
        try
        {
            session.login(target.digikabuUsername(), password);
            ParsedHeader header = HeaderParser.parse(session.fetchMainPage());
            long classId = store.linkClass(accountId, header);

            if (classesDone.add(header.className()))
            {
                try
                {
                    crawlClass(session, classId, header.className(), started);
                }
                catch (DigikabuException e)
                {
                    classesDone.remove(header.className());
                    throw e;
                }
            }

            store.storeAbsences(accountId, AbsenceParser.parse(session.fetchAbsences()), started);
            store.markSuccess(accountId, Instant.now());
            logger.info("account {} ({}) crawled in {} ms", accountId, header.className(), Instant.now().toEpochMilli() - started.toEpochMilli());
        }
        catch (DigikabuException.AuthFailed e)
        {
            logger.warn("account {}: login rejected - pausing until credentials are updated", accountId);
            store.markFailure(accountId, CrawlStatus.AUTH_FAILED, "digikabu hat Benutzername/Passwort abgelehnt", null);
        }
        catch (DigikabuException.ParseFailed e)
        {
            String url = e.getUrl() != null ? e.getUrl() : session.getLastUrl();
            logger.error("account {}: unexpected page at {}: {}", accountId, url, e.getMessage());
            store.saveDebug(accountId, url, e.getMessage(), e.getHtml(), Instant.now());
            store.markFailure(accountId, CrawlStatus.PARSE_ERROR, e.getMessage(), null);
        }
        catch (DigikabuException e)
        {
            backOff(accountId, started, e.getMessage());
        }
        catch (RuntimeException e)
        {
            logger.error("account {}: crawl failed", accountId, e);
            backOff(accountId, started, "interner Fehler: " + e);
        }
        finally
        {
            session.close();
        }
    }

    private void crawlClass(DigikabuSession session, long classId, String className, Instant now) throws DigikabuException
    {
        LocalDate today = LocalDate.now(KabuConfig.ZONE);
        ParsedWeek current = TimetableParser.parse(session.fetchTimetable(className), today);
        int changes = store.storeWeek(classId, current, now);

        // digikabu shows at most one week back and one week ahead; the date lives in the server session
        String token = current.token();
        if (token != null)
        {
            session.changeWeek(className, -WEEK, token);
            ParsedWeek previous = TimetableParser.parse(session.fetchTimetable(className), today);
            changes += store.storeWeek(classId, previous, now);

            session.changeWeek(className, WEEK, tokenOr(previous, token));
            session.changeWeek(className, WEEK, tokenOr(previous, token));
            ParsedWeek next = TimetableParser.parse(session.fetchTimetable(className), today);
            changes += store.storeWeek(classId, next, now);
        }

        store.storeCalendar(classId, ExamPlanParser.parse(session.fetchExamPlan()), now);
        if (changes > 0)
        {
            logger.info("class {}: {} timetable change(s)", className, changes);
        }
    }

    private void backOff(long accountId, Instant now, String message)
    {
        int failCount = store.nextFailCount(accountId);
        Instant next = CrawlPolicy.nextAttempt(now, config.getCrawlInterval(), failCount);
        logger.warn("account {}: {} - retry not before {}", accountId, message, next);
        store.markFailure(accountId, CrawlStatus.UNAVAILABLE, message, next);
    }

    private static String tokenOr(ParsedWeek week, String fallback)
    {
        return week.token() != null ? week.token() : fallback;
    }

    public enum RefreshResult
    {
        STARTED, COOLDOWN, BUSY, NO_ACCOUNT
    }
}
