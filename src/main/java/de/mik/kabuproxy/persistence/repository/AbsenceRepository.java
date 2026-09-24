package de.mik.kabuproxy.persistence.repository;

import de.mik.kabuproxy.persistence.entities.AbsenceEntity;
import de.mik.kabuproxy.persistence.entities.DigikabuAccountEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class AbsenceRepository
{
    @PersistenceContext(unitName = "kabu")
    private EntityManager em;

    public List<AbsenceEntity> findByAccount(long accountId)
    {
        return em.createQuery("select a from AbsenceEntity a where a.account.id = :accountId order by a.date desc", AbsenceEntity.class)
            .setParameter("accountId", accountId)
            .getResultList();
    }

    public void deleteByAccount(DigikabuAccountEntity account)
    {
        em.createQuery("delete from AbsenceEntity a where a.account = :account")
            .setParameter("account", account)
            .executeUpdate();
    }

    public void persist(AbsenceEntity absence)
    {
        em.persist(absence);
    }
}
