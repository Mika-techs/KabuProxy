package de.mik.kabuproxy.web.model;

import java.time.LocalDate;
import java.util.List;

public record WeekView(LocalDate monday, List<PeriodView> periods, List<DayView> days, boolean hasLessons)
{
    public String label()
    {
        return Formats.dayMonth(monday) + " – " + Formats.dayMonth(monday.plusDays(4));
    }

    public String gridRows()
    {
        if (periods.isEmpty())
        {
            return "grid-template-rows:var(--row-h)";
        }
        StringBuilder rows = new StringBuilder("grid-template-rows:");
        for (PeriodView period : periods)
        {
            rows.append(period.breakBefore() ? "var(--break-h) var(--row-h) " : "var(--row-h) ");
        }
        return rows.toString().trim();
    }

    public LocalDate previous()
    {
        return monday.minusWeeks(1);
    }

    public LocalDate next()
    {
        return monday.plusWeeks(1);
    }
}
