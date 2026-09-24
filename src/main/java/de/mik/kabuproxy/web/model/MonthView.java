package de.mik.kabuproxy.web.model;

import java.util.List;

public record MonthView(String title, List<CalendarEntryView> entries)
{
}
