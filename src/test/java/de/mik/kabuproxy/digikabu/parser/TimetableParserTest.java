package de.mik.kabuproxy.digikabu.parser;

import de.mik.kabuproxy.Fixtures;
import de.mik.kabuproxy.digikabu.DigikabuException;
import de.mik.kabuproxy.persistence.entities.LessonStatus;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimetableParserTest
{
    private static final LocalDate REFERENCE = LocalDate.of(2026, 9, 24);

    @Test
    void parsesClassPeriodsAndDays() throws Exception
    {
        ParsedWeek week = TimetableParser.parse(Fixtures.load("timetable_2026-09-21.html"), REFERENCE);

        assertEquals("WIT12A", week.className());
        assertNotNull(week.token());
        assertEquals(10, week.periods().size());
        assertEquals(new ParsedPeriod(1, LocalTime.of(8, 30), LocalTime.of(9, 15)), week.periods().getFirst());
        assertEquals(new ParsedPeriod(10, LocalTime.of(15, 30), LocalTime.of(16, 15)), week.periods().getLast());
        // digikabu reports 10:00-11:00, the first 15 minutes are the break
        assertEquals(new ParsedPeriod(3, LocalTime.of(10, 15), LocalTime.of(11, 0)), week.periods().get(2));

        assertEquals(5, week.days().size());
        assertEquals(LocalDate.of(2026, 9, 21), week.days().getFirst().date());
        assertEquals(DayOfWeek.FRIDAY, week.days().getLast().date().getDayOfWeek());
    }

    @Test
    void parsesSingleSplitAndChangedLessons() throws Exception
    {
        ParsedWeek week = TimetableParser.parse(Fixtures.load("timetable_2026-09-21.html"), REFERENCE);
        ParsedDay monday = week.days().getFirst();

        ParsedLesson first = monday.lessons().get(0);
        assertEquals(1, first.periodFrom());
        assertEquals(1, first.periodTo());
        assertEquals(0, first.lane());
        assertEquals(1, first.laneCount());
        assertEquals("TIS", first.teacher());
        assertEquals("C004", first.room());
        assertEquals("PuG", first.subject());
        assertEquals(LessonStatus.REGULAR, first.status());
        assertEquals("1960618", first.digikabuId());
        assertNull(first.note());

        ParsedLesson splitLeft = monday.lessons().get(1);
        assertEquals(2, splitLeft.periodFrom());
        assertEquals(3, splitLeft.periodTo());
        assertEquals(0, splitLeft.lane());
        assertEquals(2, splitLeft.laneCount());

        ParsedLesson splitRight = monday.lessons().get(2);
        assertEquals(1, splitRight.lane());
        assertEquals(2, splitRight.laneCount());
        assertEquals("C114", splitRight.room());
        assertEquals(LessonStatus.CHANGED, splitRight.status());
        assertEquals("vertretStd", splitRight.statusRaw());
        assertEquals("Änderung", splitRight.note());
    }

    @Test
    void parsesFollowingWeek() throws Exception
    {
        ParsedWeek week = TimetableParser.parse(Fixtures.load("timetable_2026-09-28.html"), REFERENCE);

        assertEquals(LocalDate.of(2026, 9, 28), week.days().getFirst().date());
        assertTrue(week.days().stream().mapToInt(d -> d.lessons().size()).sum() > 0);
    }

    @Test
    void rejectsUnknownLayout()
    {
        assertThrows(DigikabuException.ParseFailed.class, () -> TimetableParser.parse("<div>nothing here</div>", REFERENCE));
    }

    @Test
    void mapsStatusClasses()
    {
        assertEquals(LessonStatus.REGULAR, TimetableParser.statusOf(null));
        assertEquals(LessonStatus.REGULAR, TimetableParser.statusOf("regStd"));
        assertEquals(LessonStatus.CHANGED, TimetableParser.statusOf("vertretStd"));
        assertEquals(LessonStatus.CANCELLED, TimetableParser.statusOf("entfallStd"));
        assertEquals(LessonStatus.UNKNOWN, TimetableParser.statusOf("fooStd"));
    }
}
