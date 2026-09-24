package de.mik.kabuproxy.security;

import lombok.Getter;
import lombok.Setter;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;

/**
 * Who is logged in, filled once per HTTP session by {@link AccessFilter}.
 */
@Getter
@Setter
@Named
@SessionScoped
public class UserSession implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private boolean admin;
    private boolean active;

    public boolean isLoggedIn()
    {
        return userId != null;
    }
}
