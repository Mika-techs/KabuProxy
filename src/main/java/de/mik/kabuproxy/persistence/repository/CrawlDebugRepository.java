package de.mik.kabuproxy.persistence.repository;

import de.mik.kabuproxy.persistence.entities.CrawlDebugEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;

@ApplicationScoped
public class CrawlDebugRepository
{
    @PersistenceContext(unitName = "kabu")
    private EntityManager em;

    public void persist(CrawlDebugEntity debug)
    {
        em.persist(debug);
    }

    public int deleteOlderThan(Instant cutoff)
    {
        return em.createQuery("delete from CrawlDebugEntity d where d.createdAt < :cutoff")
            .setParameter("cutoff", cutoff)
            .executeUpdate();
    }
}
