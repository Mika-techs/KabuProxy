package de.mik.kabuproxy.service;

import de.mik.kabuproxy.persistence.entities.CalendarDayEntity;
import de.mik.kabuproxy.persistence.entities.DayKind;
import de.mik.kabuproxy.persistence.repository.CalendarRepository;
import de.mik.kabuproxy.web.model.CalendarEntryView;
import de.mik.kabuproxy.web.model.MonthView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableQueryServiceTest
{
    @Mock private CalendarRepository calendarRepository;

    @InjectMocks private TimetableQueryService service;

    @Test
    void defaultWeekIsNextWeekOnWeekends()
    {
        assertEquals(LocalDate.of(2026, 9, 21), TimetableQueryService.defaultMonday(LocalDate.of(2026, 9, 24)));
        assertEquals(LocalDate.of(2026, 9, 28), TimetableQueryService.defaultMonday(LocalDate.of(2026, 9, 26)));
        assertEquals(LocalDate.of(2026, 9, 28), TimetableQueryService.defaultMonday(LocalDate.of(2026, 9, 27)));
    }

    @Test
    void mergesRangesAcrossWeekendsAndDropsPlainDays()
    {
        List<CalendarDayEntity> days = new ArrayList<>();
        // Fri 09.10. no school, weekend, Mon 12.10.-Fri 16.10. no school -> one range
        days.add(day(2026, 10, 9, DayKind.NO_SCHOOL, null));
        days.add(day(2026, 10, 10, DayKind.HOLIDAY, null));
        days.add(day(2026, 10, 11, DayKind.HOLIDAY, null));
        for (int d = 12; d <= 16; d++)
        {
            days.add(day(2026, 10, d, DayKind.NO_SCHOOL, null));
        }
        // plain school day without text is dropped, exam day kept
        days.add(day(2026, 10, 19, DayKind.SCHOOL, null));
        days.add(day(2026, 10, 20, DayKind.SCHOOL, "SchA D"));
        // holidays with a name form their own range
        days.add(day(2026, 11, 2, DayKind.HOLIDAY, "Herbstferien"));
        days.add(day(2026, 11, 3, DayKind.HOLIDAY, "Herbstferien"));
        when(calendarRepository.findBetween(anyLong(), any(), any())).thenReturn(days);

        List<MonthView> months = service.loadCalendar(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30));

        assertEquals(2, months.size());
        List<CalendarEntryView> october = months.getFirst().entries();
        assertEquals(2, october.size());
        assertEquals(LocalDate.of(2026, 10, 9), october.get(0).from());
        assertEquals(LocalDate.of(2026, 10, 16), october.get(0).to());
        assertEquals("Kein Unterricht", october.get(0).text());
        assertEquals("SchA D", october.get(1).text());

        CalendarEntryView autumn = months.get(1).entries().getFirst();
        assertEquals("Herbstferien", autumn.text());
        assertEquals(LocalDate.of(2026, 11, 3), autumn.to());
    }

    private static CalendarDayEntity day(int year, int month, int dayOfMonth, DayKind kind, String text)
    {
        CalendarDayEntity entity = new CalendarDayEntity();
        entity.setDate(LocalDate.of(year, month, dayOfMonth));
        entity.setKind(kind);
        entity.setText(text);
        return entity;
    }
}
