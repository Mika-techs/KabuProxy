package de.mik.kabuproxy.persistence.repository;

import de.mik.kabuproxy.persistence.entities.UserSettingsEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

@ApplicationScoped
public class SettingsRepository
{
    @PersistenceContext(unitName = "kabu")
    private EntityManager em;

    public Optional<UserSettingsEntity> findByUserId(long userId)
    {
        return Optional.ofNullable(em.find(UserSettingsEntity.class, userId));
    }

    public void persist(UserSettingsEntity settings)
    {
        em.persist(settings);
    }
}
