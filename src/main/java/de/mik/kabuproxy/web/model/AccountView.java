package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.persistence.entities.CrawlStatus;

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
        return switch (crawlStatus)
        {
            case NEVER -> "noch nicht abgerufen";
            case OK -> "ok";
            case AUTH_FAILED -> "Login abgelehnt";
            case UNAVAILABLE -> "digikabu nicht erreichbar";
            case PARSE_ERROR -> "Seite nicht lesbar";
        };
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
