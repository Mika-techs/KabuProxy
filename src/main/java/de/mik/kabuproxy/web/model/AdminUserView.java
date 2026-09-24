package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.persistence.entities.UserStatus;

import java.time.Instant;

/**
 * One row of the admin page. Deliberately contains no absence data.
 */
public record AdminUserView(long userId, String username, String email, UserStatus status, Instant lastLoginAt, AccountView account)
{
    public String lastLoginLabel()
    {
        return Formats.relative(lastLoginAt);
    }

    public boolean hasAccount()
    {
        return account != null;
    }
}
