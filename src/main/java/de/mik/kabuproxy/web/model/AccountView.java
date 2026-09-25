package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.persistence.entities.CrawlStatus;
import de.mik.kabuproxy.web.I18n;

import java.time.Instant;

public record AccountView(
    long accountId,
    Long classId,
    String className,
    String displayName,
    String digikabuUsername,
    CrawlStatus crawlStatus,
    String lastError,
    Instant lastAttemptAt,
    Instant lastSuccessAt,
    Instant timetableUpdatedAt,
    Instant calendarUpdatedAt,
    Integer absenceFullDays,
    Integer absenceFullDaysUnexcused,
    Integer absenceHours,
    Integer absenceHoursUnexcused,
    Instant absencesUpdatedAt)
{
    public String statusLabel()
    {
        return I18n.text("crawl." + crawlStatus.name());
    }

    public String lastSuccessLabel()
    {
        return Formats.relative(lastSuccessAt);
    }

    public String lastAttemptLabel()
    {
        return Formats.relative(lastAttemptAt);
    }
}
