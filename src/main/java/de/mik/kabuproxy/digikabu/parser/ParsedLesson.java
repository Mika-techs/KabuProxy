package de.mik.kabuproxy.digikabu.parser;

import de.mik.kabuproxy.persistence.entities.LessonStatus;

/**
 * One lesson box of the timetable. {@code lane}/{@code laneCount} describe split boxes (groups in parallel).
 */
public record ParsedLesson(
    int periodFrom,
    int periodTo,
    int lane,
    int laneCount,
    String teacher,
    String room,
    String subject,
    LessonStatus status,
    String statusRaw,
    String hint,
    String note,
    String digikabuId)
{
}
