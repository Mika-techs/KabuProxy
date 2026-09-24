package de.mik.kabuproxy.service;

import de.mik.kabuproxy.persistence.entities.AppUserEntity;
import de.mik.kabuproxy.persistence.entities.UserStatus;
import de.mik.kabuproxy.persistence.repository.UserRepository;
import org.apache.logging.log4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;

@ApplicationScoped
public class UserService
{
    @Inject private Logger logger;
    @Inject private UserRepository userRepository;

    /**
     * Called on the first request of every HTTP session. Unknown subjects are registered as PENDING;
     * an admin logging in for the first time is activated right away.
     */
    @Transactional
    public AppUserEntity registerLogin(String subject, String username, String email, boolean admin)
    {
        Instant now = Instant.now();
        AppUserEntity user = userRepository.findBySubject(subject).orElse(null);
        if (user == null)
        {
            user = new AppUserEntity();
            user.setOidcSubject(subject);
            user.setStatus(admin ? UserStatus.ACTIVE : UserStatus.PENDING);
            user.setCreatedAt(now);
            user.setChangesSeenAt(now);
            userRepository.persist(user);
            logger.info("new user registered: {} ({})", username, user.getStatus());
        }
        else if (admin && user.getStatus() == UserStatus.PENDING)
        {
            user.setStatus(UserStatus.ACTIVE);
        }
        if (username != null)
        {
            user.setUsername(username);
        }
        if (email != null)
        {
            user.setEmail(email);
        }
        user.setLastLoginAt(now);
        return user;
    }

    @Transactional
    public void markChangesSeen(long userId)
    {
        userRepository.findById(userId).ifPresent(u -> u.setChangesSeenAt(Instant.now()));
    }

    @Transactional
    public Instant changesSeenAt(long userId)
    {
        return userRepository.findById(userId).map(AppUserEntity::getChangesSeenAt).orElse(null);
    }

    @Transactional
    public UserStatus status(long userId)
    {
        return userRepository.findById(userId).map(AppUserEntity::getStatus).orElse(null);
    }
}
