package de.mik.kabuproxy.crawler;

import de.mik.kabuproxy.persistence.entities.ChangeType;
import de.mik.kabuproxy.persistence.entities.LessonStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonDiffTest
{
    @Test
    void detectsAddedRemovedAndModified()
    {
        LessonSnapshot english = lesson(1, "E", "MOL", "C004", LessonStatus.REGULAR, null);
        LessonSnapshot math = lesson(2, "M", "RAU", "C004", LessonStatus.REGULAR, null);
        LessonSnapshot mathMoved = lesson(2, "M", "RAU", "C114", LessonStatus.CHANGED, "Änderung");
        LessonSnapshot german = lesson(3, "D", "HEC", "C004", LessonStatus.REGULAR, null);

        List<LessonDiff.Change> changes = LessonDiff.compare(List.of(english, math), List.of(mathMoved, german));

        assertEquals(3, changes.size());
        assertEquals(ChangeType.REMOVED, changes.get(0).type());
        assertEquals("E · MOL · C004", changes.get(0).before());
        assertNull(changes.get(0).after());
        assertEquals(ChangeType.MODIFIED, changes.get(1).type());
        assertEquals("M · RAU · C114 (Änderung)", changes.get(1).after());
        assertEquals(ChangeType.ADDED, changes.get(2).type());
    }

    @Test
    void identicalDaysHaveNoChanges()
    {
        LessonSnapshot english = lesson(1, "E", "MOL", "C004", LessonStatus.REGULAR, null);

        assertTrue(LessonDiff.compare(List.of(english), List.of(english)).isEmpty());
    }

    private static LessonSnapshot lesson(int period, String subject, String teacher, String room, LessonStatus status, String note)
    {
        return new LessonSnapshot(period, period, 0, 1, subject, teacher, room, status, null, note);
    }
}
