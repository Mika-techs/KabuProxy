package de.mik.kabuproxy.crawler;

import de.mik.kabuproxy.digikabu.parser.ParsedLesson;
import de.mik.kabuproxy.persistence.entities.LessonEntity;
import de.mik.kabuproxy.persistence.entities.LessonStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Comparable view of a lesson, independent of whether it came from the DB or a fresh crawl.
 */
public record LessonSnapshot(
    int periodFrom,
    int periodTo,
    int lane,
    int laneCount,
    String subject,
    String teacher,
    String room,
    LessonStatus status,
    String hint,
    String note)
{
    public static LessonSnapshot of(ParsedLesson lesson)
    {
        return new LessonSnapshot(lesson.periodFrom(), lesson.periodTo(), lesson.lane(), lesson.laneCount(), lesson.subject(), lesson.teacher(),
            lesson.room(), lesson.status(), lesson.hint(), lesson.note());
    }

    public static LessonSnapshot of(LessonEntity lesson)
    {
        return new LessonSnapshot(lesson.getPeriodFrom(), lesson.getPeriodTo(), lesson.getLane(), lesson.getLaneCount(), lesson.getSubject(),
            lesson.getTeacher(), lesson.getRoom(), lesson.getStatus(), lesson.getHint(), lesson.getNote());
    }

    public String key()
    {
        return periodFrom + "/" + lane + "/" + laneCount;
    }

    public boolean sameContent(LessonSnapshot other)
    {
        return periodTo == other.periodTo
            && Objects.equals(subject, other.subject)
            && Objects.equals(teacher, other.teacher)
            && Objects.equals(room, other.room)
            && status == other.status
            && Objects.equals(hint, other.hint)
            && Objects.equals(note, other.note);
    }

    /**
     * Short human text, e.g. "AEuP · RAU · C114 (Änderung)".
     */
    public String describe()
    {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, subject);
        addIfPresent(parts, teacher);
        addIfPresent(parts, room);
        String text = parts.isEmpty() ? "Stunde" : String.join(" · ", parts);
        List<String> extras = new ArrayList<>();
        if (status == LessonStatus.CANCELLED)
        {
            extras.add("entfällt");
        }
        addIfPresent(extras, hint);
        addIfPresent(extras, note == null ? null : note.replace('\n', ' '));
        return extras.isEmpty() ? text : text + " (" + String.join(", ", extras) + ")";
    }

    private static void addIfPresent(List<String> parts, String value)
    {
        if (value != null && !value.isBlank())
        {
            parts.add(value);
        }
    }
}
