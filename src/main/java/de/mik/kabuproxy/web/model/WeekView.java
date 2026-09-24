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
        return "grid-template-rows:repeat(" + Math.max(1, periods.size()) + ", var(--row-h))";
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
