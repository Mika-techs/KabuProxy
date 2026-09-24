package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.persistence.entities.DayKind;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarEntryViewTest
{
    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    @Test
    void detectsExams()
    {
        assertTrue(entry("SchA ITP ANT HECS").exam());
        assertTrue(entry("1.SA AEuP RAU/IRL").exam());
        assertTrue(entry("Ex D").exam());
        assertTrue(entry("Abschlussprüfung").exam());
    }

    @Test
    void ignoresOtherEntries()
    {
        assertFalse(entry("Ordnungsdienst").exam());
        assertFalse(entry("Sa Wandertag").exam());
        assertFalse(entry("Exkursion").exam());
        assertFalse(new CalendarEntryView(DAY, DAY, DayKind.HOLIDAY, "SchA", false, false).exam());
    }

    private static CalendarEntryView entry(String text)
    {
        return new CalendarEntryView(DAY, DAY, DayKind.SCHOOL, text, false, false);
    }
}
