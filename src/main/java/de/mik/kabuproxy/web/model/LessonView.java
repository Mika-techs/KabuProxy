package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.persistence.entities.LessonStatus;

/**
 * A lesson as rendered. The desktop grid uses 6 sub-columns per day so halves and thirds both fit.
 */
public record LessonView(
    int periodFrom,
    int periodTo,
    int lane,
    int laneCount,
    String subject,
    String teacher,
    String room,
    LessonStatus status,
    String hint,
    String note,
    String timeLabel)
{
    private static final int SUB_COLUMNS = 6;

    public String gridStyle()
    {
        int span = SUB_COLUMNS / Math.max(1, Math.min(laneCount, SUB_COLUMNS));
        int column = lane * span + 1;
        return "grid-row:" + periodFrom + " / " + (periodTo + 1) + ";grid-column:" + column + " / span " + span;
    }

    public String cssClass()
    {
        return switch (status)
        {
            case REGULAR -> "lesson";
            case CHANGED -> "lesson lesson--changed";
            case CANCELLED -> "lesson lesson--cancelled";
            case UNKNOWN -> "lesson lesson--unknown";
        };
    }

    public String periodLabel()
    {
        return periodFrom == periodTo ? Integer.toString(periodFrom) : periodFrom + "–" + periodTo;
    }

    public boolean changed()
    {
        return status != LessonStatus.REGULAR;
    }

    public boolean singlePeriod()
    {
        return periodFrom == periodTo;
    }
}
