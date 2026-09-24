package de.mik.kabuproxy.web.model;

/**
 * A period row of the desktop grid. {@code row} is the grid row, which differs from {@code period} once a break row
 * was inserted before it.
 */
public record PeriodView(int period, int row, boolean breakBefore, String start, String end)
{
    public String gridStyle()
    {
        return "grid-row:" + row;
    }
}
