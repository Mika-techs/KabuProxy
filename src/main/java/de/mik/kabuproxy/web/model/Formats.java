package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.config.KabuConfig;
import de.mik.kabuproxy.web.I18n;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;

/**
 * Date/time labels in the current UI locale (see {@link I18n}); times are H:mm in every locale, kabu.js parses them.
 */
public final class Formats
{
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm");

    private Formats()
    {
    }

    public static String dayMonth(LocalDate date)
    {
        return date == null ? "" : format("format.dayMonth", date);
    }

    public static String date(LocalDate date)
    {
        return date == null ? "" : format("format.date", date);
    }

    public static String time(LocalTime time)
    {
        return time == null ? "" : TIME.format(time);
    }

    public static String weekdayShort(LocalDate date)
    {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, I18n.locale()).replace(".", "");
    }

    public static String weekdayLong(LocalDate date)
    {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, I18n.locale());
    }

    public static String monthYear(LocalDate date)
    {
        return date.getMonth().getDisplayName(TextStyle.FULL, I18n.locale()) + " " + date.getYear();
    }

    /**
     * "heute 14:30", "gestern 07:10" or "21.09. 14:30" (English: "today 14:30", …, "21 Sep 14:30").
     */
    public static String relative(Instant instant)
    {
        if (instant == null)
        {
            return I18n.text("relative.never");
        }
        ZonedDateTime time = instant.atZone(KabuConfig.ZONE);
        LocalDate today = LocalDate.now(KabuConfig.ZONE);
        if (time.toLocalDate().equals(today))
        {
            return I18n.text("relative.today", TIME.format(time));
        }
        if (time.toLocalDate().equals(today.minusDays(1)))
        {
            return I18n.text("relative.yesterday", TIME.format(time));
        }
        return format("format.dateTime", time);
    }

    private static String format(String patternKey, TemporalAccessor value)
    {
        return DateTimeFormatter.ofPattern(I18n.text(patternKey), I18n.locale()).format(value);
    }
}
