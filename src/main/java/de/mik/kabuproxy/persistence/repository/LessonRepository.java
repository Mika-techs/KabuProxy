package de.mik.kabuproxy.persistence.repository;

import de.mik.kabuproxy.persistence.entities.LessonChangeEntity;
import de.mik.kabuproxy.persistence.entities.LessonEntity;
import de.mik.kabuproxy.persistence.entities.SchoolClassEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class LessonRepository
{
    private static final int MAX_CHANGES = 100;

    @PersistenceContext(unitName = "kabu")
    private EntityManager em;

    public List<LessonEntity> findBetween(long classId, LocalDate from, LocalDate to)
    {
        return em.createQuery("select l from LessonEntity l where l.schoolClass.id = :classId and l.date between :from and :to"
                    + " order by l.date, l.periodFrom, l.lane",
                LessonEntity.class)
            .setParameter("classId", classId)
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
    }

    public void deleteDay(SchoolClassEntity schoolClass, LocalDate date)
    {
        em.createQuery("delete from LessonEntity l where l.schoolClass = :schoolClass and l.date = :date")
            .setParameter("schoolClass", schoolClass)
            .setParameter("date", date)
            .executeUpdate();
    }

    public void persist(LessonEntity lesson)
    {
        em.persist(lesson);
    }

    public void persist(LessonChangeEntity change)
    {
        em.persist(change);
    }

    /**
     * Changes detected after {@code since} that concern lessons from {@code fromDate} on (past lessons are irrelevant).
     */
    public List<LessonChangeEntity> findChanges(long classId, Instant since, LocalDate fromDate)
    {
        return em.createQuery("select c from LessonChangeEntity c where c.schoolClass.id = :classId and c.detectedAt > :since"
                    + " and c.date >= :fromDate order by c.date, c.periodFrom, c.detectedAt",
                LessonChangeEntity.class)
            .setParameter("classId", classId)
            .setParameter("since", since)
            .setParameter("fromDate", fromDate)
            .setMaxResults(MAX_CHANGES)
            .getResultList();
    }
}
