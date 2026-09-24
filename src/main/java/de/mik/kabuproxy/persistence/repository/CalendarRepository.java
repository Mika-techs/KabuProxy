package de.mik.kabuproxy.persistence.repository;

import de.mik.kabuproxy.persistence.entities.CalendarDayEntity;
import de.mik.kabuproxy.persistence.entities.SchoolClassEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class CalendarRepository
{
    @PersistenceContext(unitName = "kabu")
    private EntityManager em;

    public List<CalendarDayEntity> findBetween(long classId, LocalDate from, LocalDate to)
    {
        return em.createQuery("select d from CalendarDayEntity d where d.schoolClass.id = :classId and d.date between :from and :to order by d.date",
                CalendarDayEntity.class)
            .setParameter("classId", classId)
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
    }

    public void deleteBetween(SchoolClassEntity schoolClass, LocalDate from, LocalDate to)
    {
        em.createQuery("delete from CalendarDayEntity d where d.schoolClass = :schoolClass and d.date between :from and :to")
            .setParameter("schoolClass", schoolClass)
            .setParameter("from", from)
            .setParameter("to", to)
            .executeUpdate();
    }

    public void persist(CalendarDayEntity day)
    {
        em.persist(day);
    }
}
