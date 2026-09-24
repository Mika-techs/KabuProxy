package de.mik.kabuproxy.persistence.repository;

import de.mik.kabuproxy.persistence.entities.AppUserEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserRepository
{
    @PersistenceContext(unitName = "kabu")
    private EntityManager em;

    public Optional<AppUserEntity> findById(long id)
    {
        return Optional.ofNullable(em.find(AppUserEntity.class, id));
    }

    public Optional<AppUserEntity> findBySubject(String subject)
    {
        return em.createQuery("select u from AppUserEntity u where u.oidcSubject = :subject", AppUserEntity.class)
            .setParameter("subject", subject)
            .getResultStream()
            .findFirst();
    }

    public List<AppUserEntity> findAll()
    {
        return em.createQuery("select u from AppUserEntity u order by u.status, u.username", AppUserEntity.class).getResultList();
    }

    public void persist(AppUserEntity user)
    {
        em.persist(user);
    }

    public void delete(AppUserEntity user)
    {
        em.remove(em.contains(user) ? user : em.merge(user));
    }
}
