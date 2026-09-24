package de.mik.kabuproxy.persistence.repository;

import de.mik.kabuproxy.persistence.entities.CrawlStatus;
import de.mik.kabuproxy.persistence.entities.DigikabuAccountEntity;
import de.mik.kabuproxy.persistence.entities.UserStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AccountRepository
{
    @PersistenceContext(unitName = "kabu")
    private EntityManager em;

    public Optional<DigikabuAccountEntity> findById(long id)
    {
        return Optional.ofNullable(em.find(DigikabuAccountEntity.class, id));
    }

    public Optional<DigikabuAccountEntity> findByUserId(long userId)
    {
        return em.createQuery("select a from DigikabuAccountEntity a join fetch a.user left join fetch a.schoolClass where a.user.id = :userId",
                DigikabuAccountEntity.class)
            .setParameter("userId", userId)
            .getResultStream()
            .findFirst();
    }

    public List<DigikabuAccountEntity> findAllWithUser()
    {
        return em.createQuery("select a from DigikabuAccountEntity a join fetch a.user left join fetch a.schoolClass", DigikabuAccountEntity.class)
            .getResultList();
    }

    /**
     * Accounts the scheduler may crawl now: user active, credentials not known-bad, back-off elapsed.
     * Healthy accounts first, so class data is fetched with the most reliable login.
     */
    public List<DigikabuAccountEntity> findCrawlable(Instant now)
    {
        return em.createQuery("select a from DigikabuAccountEntity a join fetch a.user u left join fetch a.schoolClass"
                    + " where u.status = :active and a.crawlStatus <> :authFailed and (a.nextAttemptAt is null or a.nextAttemptAt <= :now)"
                    + " order by a.failCount, a.lastSuccessAt desc",
                DigikabuAccountEntity.class)
            .setParameter("active", UserStatus.ACTIVE)
            .setParameter("authFailed", CrawlStatus.AUTH_FAILED)
            .setParameter("now", now)
            .getResultList();
    }

    public void persist(DigikabuAccountEntity account)
    {
        em.persist(account);
    }
}
