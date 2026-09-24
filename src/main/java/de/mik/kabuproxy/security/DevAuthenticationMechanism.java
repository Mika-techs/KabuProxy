package de.mik.kabuproxy.security;

import de.mik.kabuproxy.config.KabuConfig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.CallerPrincipal;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * KABU_DEV_AUTH=true only: logs every request in as an admin without asking anyone. For local development before
 * Authentik is wired up. Never enable this on a reachable server.
 */
@DevLogin
@ApplicationScoped
public class DevAuthenticationMechanism implements HttpAuthenticationMechanism
{
    public static final String DEV_SUBJECT = "dev-admin";

    @Inject private KabuConfig config;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext context)
    {
        return context.notifyContainerAboutLogin(new CallerPrincipal(DEV_SUBJECT), Set.of(config.getAdminGroup()));
    }
}
