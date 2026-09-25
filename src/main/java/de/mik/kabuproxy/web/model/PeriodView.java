package de.mik.kabuproxy.web.model;

/**
 * A period row of the desktop grid. {@code row} is the grid row, which differs from {@code period} once a break row
 * was inserted before it; {@code breakStart} is the start of that break (empty without one).
 */
public record PeriodView(int period, int row, boolean breakBefore, String breakStart, String start, String end)
{
    public String gridStyle()
    {
        return "grid-row:" + row;
    }

    public String breakStyle()
    {
        return "grid-row:" + (row - 1) + ";grid-column:1 / -1";
    }

    public String breakLabel()
    {
        return breakStart + "–" + start;
    }
}
