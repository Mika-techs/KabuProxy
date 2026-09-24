package de.mik.kabuproxy.crawler;

import de.mik.kabuproxy.digikabu.parser.ParsedAbsence;
import de.mik.kabuproxy.digikabu.parser.ParsedAbsences;
import de.mik.kabuproxy.digikabu.parser.ParsedCalendarDay;
import de.mik.kabuproxy.digikabu.parser.ParsedDay;
import de.mik.kabuproxy.digikabu.parser.ParsedHeader;
import de.mik.kabuproxy.digikabu.parser.ParsedLesson;
import de.mik.kabuproxy.digikabu.parser.ParsedPeriod;
import de.mik.kabuproxy.digikabu.parser.ParsedWeek;
import de.mik.kabuproxy.persistence.entities.AbsenceEntity;
import de.mik.kabuproxy.persistence.entities.CalendarDayEntity;
import de.mik.kabuproxy.persistence.entities.CrawlDebugEntity;
import de.mik.kabuproxy.persistence.entities.CrawlStatus;
import de.mik.kabuproxy.persistence.entities.DigikabuAccountEntity;
import de.mik.kabuproxy.persistence.entities.LessonChangeEntity;
import de.mik.kabuproxy.persistence.entities.LessonEntity;
import de.mik.kabuproxy.persistence.entities.PeriodSlotEntity;
import de.mik.kabuproxy.persistence.entities.SchoolClassEntity;
import de.mik.kabuproxy.persistence.repository.AbsenceRepository;
import de.mik.kabuproxy.persistence.repository.AccountRepository;
import de.mik.kabuproxy.persistence.repository.CalendarRepository;
import de.mik.kabuproxy.persistence.repository.CrawlDebugRepository;
import de.mik.kabuproxy.persistence.repository.LessonRepository;
import de.mik.kabuproxy.persistence.repository.SchoolClassRepository;
import org.apache.logging.log4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * All DB writes of the crawler. Each public method is its own transaction, so a failure in a later crawl step keeps
 * what was already stored.
 */
@ApplicationScoped
public class CrawlStore
{
    private static final int MAX_ERROR_LENGTH = 1000;
    private static final Duration DEBUG_RETENTION = Duration.ofDays(7);

    @Inject private Logger logger;
    @Inject private AccountRepository accountRepository;
    @Inject private SchoolClassRepository schoolClassRepository;
    @Inject private LessonRepository lessonRepository;
    @Inject private CalendarRepository calendarRepository;
    @Inject private AbsenceRepository absenceRepository;
    @Inject private CrawlDebugRepository crawlDebugRepository;

    /**
     * Stores name/class from the navbar and returns the class id.
     */
    @Transactional
    public long linkClass(long accountId, ParsedHeader header)
    {
        DigikabuAccountEntity account = accountRepository.findById(accountId).orElseThrow();
        SchoolClassEntity schoolClass = schoolClassRepository.findOrCreate(header.className());
        account.setSchoolClass(schoolClass);
        account.setDisplayName(header.displayName());
        return schoolClass.getId();
    }

    /**
     * Replaces the lessons of every day contained in {@code week} and logs differences to the previous state.
     * A day seen for the first time produces no change entries.
     *
     * @return number of detected changes
     */
    @Transactional
    public int storeWeek(long classId, ParsedWeek week, Instant now)
    {
        SchoolClassEntity schoolClass = schoolClassRepository.findById(classId).orElseThrow();
        if (!week.periods().isEmpty())
        {
            storePeriods(schoolClass, week.periods());
        }

        LocalDate first = week.days().stream().map(ParsedDay::date).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate last = week.days().stream().map(ParsedDay::date).max(Comparator.naturalOrder()).orElseThrow();
        Map<LocalDate, List<LessonEntity>> existing = lessonRepository.findBetween(classId, first, last).stream()
            .collect(Collectors.groupingBy(LessonEntity::getDate));

        int changeCount = 0;
        for (ParsedDay day : week.days())
        {
            List<LessonEntity> before = existing.getOrDefault(day.date(), List.of());
            if (!before.isEmpty())
            {
                List<LessonDiff.Change> changes = LessonDiff.compare(
                    before.stream().map(LessonSnapshot::of).toList(),
                    day.lessons().stream().map(LessonSnapshot::of).toList());
                for (LessonDiff.Change change : changes)
                {
                    lessonRepository.persist(toEntity(schoolClass, day.date(), change, now));
                }
                changeCount += changes.size();
            }

            lessonRepository.deleteDay(schoolClass, day.date());
            for (ParsedLesson lesson : day.lessons())
            {
                lessonRepository.persist(toEntity(schoolClass, day.date(), lesson, now));
            }
        }
        schoolClass.setTimetableUpdatedAt(now);
        return changeCount;
    }

    @Transactional
    public void storeCalendar(long classId, List<ParsedCalendarDay> days, Instant now)
    {
        if (days.isEmpty())
        {
            return;
        }
        SchoolClassEntity schoolClass = schoolClassRepository.findById(classId).orElseThrow();
        LocalDate first = days.stream().map(ParsedCalendarDay::date).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate last = days.stream().map(ParsedCalendarDay::date).max(Comparator.naturalOrder()).orElseThrow();
        calendarRepository.deleteBetween(schoolClass, first, last);
        for (ParsedCalendarDay day : days)
        {
            CalendarDayEntity entity = new CalendarDayEntity();
            entity.setSchoolClass(schoolClass);
            entity.setDate(day.date());
            entity.setKind(day.kind());
            entity.setText(day.text());
            calendarRepository.persist(entity);
        }
        schoolClass.setCalendarUpdatedAt(now);
    }

    @Transactional
    public void storeAbsences(long accountId, ParsedAbsences absences, Instant now)
    {
        DigikabuAccountEntity account = accountRepository.findById(accountId).orElseThrow();
        absenceRepository.deleteByAccount(account);
        for (ParsedAbsence parsed : absences.entries())
        {
            AbsenceEntity entity = new AbsenceEntity();
            entity.setAccount(account);
            entity.setDate(parsed.date());
            entity.setFromText(parsed.from());
            entity.setToText(parsed.to());
            entity.setRemark(parsed.remark());
            entity.setKind(parsed.kind());
            entity.setExcused(parsed.excused());
            absenceRepository.persist(entity);
        }
        account.setAbsenceFullDays(absences.fullDays());
        account.setAbsenceFullDaysUnexcused(absences.fullDaysUnexcused());
        account.setAbsenceHours(absences.hours());
        account.setAbsenceHoursUnexcused(absences.hoursUnexcused());
        account.setAbsencesUpdatedAt(now);
    }

    @Transactional
    public List<CrawlTarget> loadCrawlable(Instant now)
    {
        return accountRepository.findCrawlable(now).stream().map(CrawlStore::toTarget).toList();
    }

    @Transactional
    public Optional<CrawlTarget> loadByAccount(long accountId)
    {
        return accountRepository.findById(accountId).map(CrawlStore::toTarget);
    }

    @Transactional
    public Optional<CrawlTarget> loadByUser(long userId)
    {
        return accountRepository.findByUserId(userId).map(CrawlStore::toTarget);
    }

    @Transactional
    public void markAttempt(long accountId, Instant now)
    {
        accountRepository.findById(accountId).orElseThrow().setLastAttemptAt(now);
    }

    @Transactional
    public void markSuccess(long accountId, Instant now)
    {
        DigikabuAccountEntity account = accountRepository.findById(accountId).orElseThrow();
        account.setCrawlStatus(CrawlStatus.OK);
        account.setFailCount(0);
        account.setLastError(null);
        account.setLastSuccessAt(now);
        account.setNextAttemptAt(null);
    }

    /**
     * @param nextAttempt earliest next scheduled crawl, {@code null} = next cycle
     */
    @Transactional
    public void markFailure(long accountId, CrawlStatus status, String error, Instant nextAttempt)
    {
        DigikabuAccountEntity account = accountRepository.findById(accountId).orElseThrow();
        account.setCrawlStatus(status);
        account.setFailCount(account.getFailCount() + 1);
        account.setLastError(truncate(error));
        account.setNextAttemptAt(nextAttempt);
    }

    /**
     * Current fail count + 1, used to compute the back-off before {@link #markFailure}.
     */
    @Transactional
    public int nextFailCount(long accountId)
    {
        return accountRepository.findById(accountId).orElseThrow().getFailCount() + 1;
    }

    @Transactional
    public void saveDebug(Long accountId, String url, String error, String html, Instant now)
    {
        CrawlDebugEntity debug = new CrawlDebugEntity();
        debug.setAccount(accountId == null ? null : accountRepository.findById(accountId).orElse(null));
        debug.setUrl(url);
        debug.setError(truncate(error));
        debug.setHtml(html);
        debug.setCreatedAt(now);
        crawlDebugRepository.persist(debug);
        int pruned = crawlDebugRepository.deleteOlderThan(now.minus(DEBUG_RETENTION));
        if (pruned > 0)
        {
            logger.debug("pruned {} old crawl debug rows", pruned);
        }
    }

    private void storePeriods(SchoolClassEntity schoolClass, List<ParsedPeriod> periods)
    {
        List<PeriodSlotEntity> slots = new ArrayList<>();
        for (ParsedPeriod period : periods)
        {
            PeriodSlotEntity slot = new PeriodSlotEntity();
            slot.setSchoolClass(schoolClass);
            slot.setPeriod(period.period());
            slot.setStartTime(period.start());
            slot.setEndTime(period.end());
            slots.add(slot);
        }
        schoolClassRepository.replacePeriods(schoolClass, slots);
    }

    private static CrawlTarget toTarget(DigikabuAccountEntity account)
    {
        SchoolClassEntity schoolClass = account.getSchoolClass();
        return new CrawlTarget(account.getId(), account.getDigikabuUsername(), account.getPasswordEnc(),
            schoolClass == null ? null : schoolClass.getName(), account.getLastAttemptAt());
    }

    private static LessonEntity toEntity(SchoolClassEntity schoolClass, LocalDate date, ParsedLesson lesson, Instant now)
    {
        LessonEntity entity = new LessonEntity();
        entity.setSchoolClass(schoolClass);
        entity.setDate(date);
        entity.setPeriodFrom(lesson.periodFrom());
        entity.setPeriodTo(lesson.periodTo());
        entity.setLane(lesson.lane());
        entity.setLaneCount(lesson.laneCount());
        entity.setTeacher(lesson.teacher());
        entity.setRoom(lesson.room());
        entity.setSubject(lesson.subject());
        entity.setStatus(lesson.status());
        entity.setStatusRaw(lesson.statusRaw());
        entity.setHint(lesson.hint());
        entity.setNote(lesson.note());
        entity.setDigikabuId(lesson.digikabuId());
        entity.setUpdatedAt(now);
        return entity;
    }

    private static LessonChangeEntity toEntity(SchoolClassEntity schoolClass, LocalDate date, LessonDiff.Change change, Instant now)
    {
        LessonChangeEntity entity = new LessonChangeEntity();
        entity.setSchoolClass(schoolClass);
        entity.setDate(date);
        entity.setPeriodFrom(change.periodFrom());
        entity.setPeriodTo(change.periodTo());
        entity.setChangeType(change.type());
        entity.setBeforeText(change.before());
        entity.setAfterText(change.after());
        entity.setDetectedAt(now);
        return entity;
    }

    private static String truncate(String value)
    {
        if (value == null || value.length() <= MAX_ERROR_LENGTH)
        {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }
}
