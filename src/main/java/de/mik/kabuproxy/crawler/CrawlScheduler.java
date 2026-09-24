package de.mik.kabuproxy.crawler;

import de.mik.kabuproxy.config.KabuConfig;
import org.apache.logging.log4j.Logger;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class CrawlScheduler
{
    private static final long INITIAL_DELAY_SECONDS = 30L;

    @Inject private Logger logger;
    @Inject private KabuConfig config;
    @Inject private CrawlService crawlService;

    @Resource
    private ManagedScheduledExecutorService scheduler;

    private ScheduledFuture<?> future;

    public void start()
    {
        if (!config.isCrawlEnabled())
        {
            logger.warn("crawler disabled (KABU_CRAWL_ENABLED=false)");
            return;
        }
        long interval = config.getCrawlInterval().toSeconds();
        future = scheduler.scheduleAtFixedRate(this::tick, INITIAL_DELAY_SECONDS, interval, TimeUnit.SECONDS);
        logger.info("crawler scheduled every {} min between {} and {}", config.getCrawlIntervalMinutes(), config.getCrawlActiveFrom(),
            config.getCrawlActiveTo());
    }

    @PreDestroy
    void stop()
    {
        if (future != null)
        {
            future.cancel(false);
        }
    }

    private void tick()
    {
        // an exception escaping here would silently cancel all future runs
        try
        {
            if (CrawlPolicy.isActive(LocalTime.now(KabuConfig.ZONE), config.getCrawlActiveFromTime(), config.getCrawlActiveToTime()))
            {
                crawlService.runCycle();
            }
        }
        catch (RuntimeException e)
        {
            logger.error("crawl cycle failed", e);
        }
    }
}
