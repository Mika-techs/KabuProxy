package de.mik.kabuproxy.persistence.repository;

import de.mik.kabuproxy.persistence.entities.PeriodSlotEntity;
import de.mik.kabuproxy.persistence.entities.SchoolClassEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SchoolClassRepository
{
    @PersistenceContext(unitName = "kabu")
    private EntityManager em;

    public Optional<SchoolClassEntity> findById(long id)
    {
        return Optional.ofNullable(em.find(SchoolClassEntity.class, id));
    }

    public SchoolClassEntity findOrCreate(String name)
    {
        return em.createQuery("select c from SchoolClassEntity c where c.name = :name", SchoolClassEntity.class)
            .setParameter("name", name)
            .getResultStream()
            .findFirst()
            .orElseGet(() ->
            {
                SchoolClassEntity created = new SchoolClassEntity();
                created.setName(name);
                em.persist(created);
                return created;
            });
    }

    public List<PeriodSlotEntity> findPeriods(long classId)
    {
        return em.createQuery("select p from PeriodSlotEntity p where p.schoolClass.id = :classId order by p.period", PeriodSlotEntity.class)
            .setParameter("classId", classId)
            .getResultList();
    }

    public void replacePeriods(SchoolClassEntity schoolClass, List<PeriodSlotEntity> periods)
    {
        em.createQuery("delete from PeriodSlotEntity p where p.schoolClass = :schoolClass")
            .setParameter("schoolClass", schoolClass)
            .executeUpdate();
        periods.forEach(em::persist);
    }
}
