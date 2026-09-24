package de.mik.kabuproxy.web.model;

public record PeriodView(int period, String start, String end)
{
    public String gridStyle()
    {
        return "grid-row:" + period;
    }
}
