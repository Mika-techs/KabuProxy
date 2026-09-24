package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.config.KabuConfig;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public final class Formats
{
    private static final Locale DE = Locale.GERMANY;
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM.", DE);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", DE);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm", DE);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM. HH:mm", DE);

    private Formats()
    {
    }

    public static String dayMonth(LocalDate date)
    {
        return date == null ? "" : DAY_MONTH.format(date);
    }

    public static String date(LocalDate date)
    {
        return date == null ? "" : DATE.format(date);
    }

    public static String time(LocalTime time)
    {
        return time == null ? "" : TIME.format(time);
    }

    public static String weekdayShort(LocalDate date)
    {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, DE).replace(".", "");
    }

    public static String weekdayLong(LocalDate date)
    {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, DE);
    }

    /**
     * "heute 14:30", "gestern 07:10" or "21.09. 14:30".
     */
    public static String relative(Instant instant)
    {
        if (instant == null)
        {
            return "nie";
        }
        ZonedDateTime time = instant.atZone(KabuConfig.ZONE);
        LocalDate today = LocalDate.now(KabuConfig.ZONE);
        if (time.toLocalDate().equals(today))
        {
            return "heute " + TIME.format(time);
        }
        if (time.toLocalDate().equals(today.minusDays(1)))
        {
            return "gestern " + TIME.format(time);
        }
        return DATE_TIME.format(time);
    }
}
