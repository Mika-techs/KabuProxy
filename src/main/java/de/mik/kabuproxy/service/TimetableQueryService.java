package de.mik.kabuproxy.service;

import de.mik.kabuproxy.config.KabuConfig;
import de.mik.kabuproxy.persistence.entities.CalendarDayEntity;
import de.mik.kabuproxy.persistence.entities.DayKind;
import de.mik.kabuproxy.persistence.entities.LessonChangeEntity;
import de.mik.kabuproxy.persistence.entities.LessonEntity;
import de.mik.kabuproxy.persistence.entities.PeriodSlotEntity;
import de.mik.kabuproxy.persistence.repository.CalendarRepository;
import de.mik.kabuproxy.persistence.repository.LessonRepository;
import de.mik.kabuproxy.persistence.repository.SchoolClassRepository;
import de.mik.kabuproxy.web.model.CalendarEntryView;
import de.mik.kabuproxy.web.model.ChangeView;
import de.mik.kabuproxy.web.model.DayView;
import de.mik.kabuproxy.web.model.Formats;
import de.mik.kabuproxy.web.model.LessonView;
import de.mik.kabuproxy.web.model.MonthView;
import de.mik.kabuproxy.web.model.PeriodView;
import de.mik.kabuproxy.web.model.WeekView;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class TimetableQueryService
{
    private static final int SCHOOL_DAYS = 5;
    private static final int DEFAULT_PERIODS = 10;

    @Inject private SchoolClassRepository schoolClassRepository;
    @Inject private LessonRepository lessonRepository;
    @Inject private CalendarRepository calendarRepository;

    /**
     * Monday of the week to show by default: this week, or next week on weekends.
     */
    public static LocalDate defaultMonday(LocalDate today)
    {
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY)
        {
            return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    @Transactional
    public WeekView loadWeek(long classId, LocalDate monday)
    {
        LocalDate friday = monday.plusDays(SCHOOL_DAYS - 1);
        LocalDate today = LocalDate.now(KabuConfig.ZONE);

        List<PeriodSlotEntity> slots = schoolClassRepository.findPeriods(classId);
        Map<Integer, PeriodSlotEntity> slotByPeriod = slots.stream().collect(Collectors.toMap(PeriodSlotEntity::getPeriod, s -> s));

        Map<LocalDate, List<LessonEntity>> lessonsByDay = lessonRepository.findBetween(classId, monday, friday).stream()
            .collect(Collectors.groupingBy(LessonEntity::getDate));
        Map<LocalDate, CalendarDayEntity> calendar = calendarRepository.findBetween(classId, monday, friday).stream()
            .collect(Collectors.toMap(CalendarDayEntity::getDate, d -> d));

        int maxPeriod = lessonsByDay.values().stream().flatMap(List::stream).mapToInt(LessonEntity::getPeriodTo).max().orElse(0);
        int periodCount = Math.max(maxPeriod, slots.isEmpty() ? DEFAULT_PERIODS : slots.getLast().getPeriod());

        List<PeriodView> periods = new ArrayList<>();
        Map<Integer, Integer> rowByPeriod = new HashMap<>();
        int row = 0;
        for (int period = 1; period <= periodCount; period++)
        {
            PeriodSlotEntity slot = slotByPeriod.get(period);
            PeriodSlotEntity previous = slotByPeriod.get(period - 1);
            // a gap between two periods is a break - it gets its own (small) grid row
            boolean breakBefore = slot != null && previous != null && slot.getStartTime().isAfter(previous.getEndTime());
            row += breakBefore ? 2 : 1;
            rowByPeriod.put(period, row);
            periods.add(new PeriodView(period, row, breakBefore, slot == null ? "" : Formats.time(slot.getStartTime()),
                slot == null ? "" : Formats.time(slot.getEndTime())));
        }

        List<DayView> days = new ArrayList<>();
        boolean hasLessons = false;
        for (int i = 0; i < SCHOOL_DAYS; i++)
        {
            LocalDate date = monday.plusDays(i);
            List<LessonView> lessons = lessonsByDay.getOrDefault(date, List.of()).stream()
                .map(l -> toView(l, slotByPeriod, rowByPeriod))
                .toList();
            hasLessons |= !lessons.isEmpty();
            CalendarDayEntity calendarDay = calendar.get(date);
            days.add(new DayView(date, date.equals(today), lessons,
                calendarDay == null ? null : calendarDay.getKind(),
                calendarDay == null ? null : calendarDay.getText()));
        }
        return new WeekView(monday, periods, days, hasLessons);
    }

    /**
     * Changes to today's or future lessons detected after {@code since}.
     */
    @Transactional
    public List<ChangeView> changesSince(long classId, Instant since)
    {
        if (since == null)
        {
            return List.of();
        }
        LocalDate today = LocalDate.now(KabuConfig.ZONE);
        Map<Integer, PeriodSlotEntity> slots = schoolClassRepository.findPeriods(classId).stream()
            .collect(Collectors.toMap(PeriodSlotEntity::getPeriod, s -> s));
        List<ChangeView> views = new ArrayList<>();
        for (LessonChangeEntity change : lessonRepository.findChanges(classId, since, today))
        {
            String period = change.getPeriodFrom() == change.getPeriodTo()
                ? change.getPeriodFrom() + ". Std"
                : change.getPeriodFrom() + ".–" + change.getPeriodTo() + ". Std";
            PeriodSlotEntity slot = slots.get(change.getPeriodFrom());
            if (slot != null)
            {
                period += " · " + Formats.time(slot.getStartTime());
            }
            views.add(new ChangeView(Formats.weekdayShort(change.getDate()) + " " + Formats.dayMonth(change.getDate()), period, change.getChangeType(),
                change.getBeforeText(), change.getAfterText(), Formats.relative(change.getDetectedAt())));
        }
        return views;
    }

    /**
     * Calendar grouped by month; holiday / no-school stretches are merged into ranges, plain school days without text
     * are left out.
     */
    @Transactional
    public List<MonthView> loadCalendar(long classId, LocalDate from, LocalDate to)
    {
        LocalDate today = LocalDate.now(KabuConfig.ZONE);
        List<CalendarEntryView> entries = new ArrayList<>();
        CalendarDayEntity rangeStart = null;
        CalendarDayEntity rangeEnd = null;
        for (CalendarDayEntity day : calendarRepository.findBetween(classId, from, to))
        {
            // weekends are noise (always "free") - skip them, and let ranges continue across them
            if (isWeekend(day.getDate()))
            {
                continue;
            }
            boolean mergeable = day.getKind() != DayKind.SCHOOL;
            if (rangeStart != null && mergeable && day.getKind() == rangeStart.getKind() && Objects.equals(day.getText(), rangeStart.getText())
                && day.getDate().equals(nextSchoolDay(rangeEnd.getDate())))
            {
                rangeEnd = day;
                continue;
            }
            if (rangeStart != null)
            {
                entries.add(entry(rangeStart, rangeEnd, today));
                rangeStart = null;
            }
            if (mergeable)
            {
                rangeStart = day;
                rangeEnd = day;
            }
            else if (day.getText() != null)
            {
                entries.add(entry(day, day, today));
            }
        }
        if (rangeStart != null)
        {
            entries.add(entry(rangeStart, rangeEnd, today));
        }

        Map<String, List<CalendarEntryView>> byMonth = new LinkedHashMap<>();
        for (CalendarEntryView entry : entries)
        {
            String title = entry.from().getMonth().getDisplayName(TextStyle.FULL, Locale.GERMANY) + " " + entry.from().getYear();
            byMonth.computeIfAbsent(title, k -> new ArrayList<>()).add(entry);
        }
        return byMonth.entrySet().stream().map(e -> new MonthView(e.getKey(), e.getValue())).toList();
    }

    private static boolean isWeekend(LocalDate date)
    {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private static LocalDate nextSchoolDay(LocalDate date)
    {
        LocalDate next = date.plusDays(1);
        while (isWeekend(next))
        {
            next = next.plusDays(1);
        }
        return next;
    }

    private static CalendarEntryView entry(CalendarDayEntity start, CalendarDayEntity end, LocalDate today)
    {
        boolean containsToday = !today.isBefore(start.getDate()) && !today.isAfter(end.getDate());
        String text = start.getText();
        if (text == null && start.getKind() == DayKind.NO_SCHOOL)
        {
            text = "Kein Unterricht";
        }
        return new CalendarEntryView(start.getDate(), end.getDate(), start.getKind(), text, end.getDate().isBefore(today), containsToday);
    }

    private static LessonView toView(LessonEntity lesson, Map<Integer, PeriodSlotEntity> slots, Map<Integer, Integer> rowByPeriod)
    {
        PeriodSlotEntity first = slots.get(lesson.getPeriodFrom());
        PeriodSlotEntity last = slots.get(lesson.getPeriodTo());
        String time = first == null || last == null ? "" : Formats.time(first.getStartTime()) + "–" + Formats.time(last.getEndTime());
        return new LessonView(lesson.getPeriodFrom(), lesson.getPeriodTo(), rowByPeriod.getOrDefault(lesson.getPeriodFrom(), lesson.getPeriodFrom()),
            rowByPeriod.getOrDefault(lesson.getPeriodTo(), lesson.getPeriodTo()), lesson.getLane(), lesson.getLaneCount(), lesson.getSubject(),
            lesson.getTeacher(), lesson.getRoom(), lesson.getStatus(), lesson.getHint(), lesson.getNote(), time);
    }
}
