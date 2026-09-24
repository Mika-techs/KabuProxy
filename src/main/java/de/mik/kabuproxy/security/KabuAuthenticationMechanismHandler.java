package de.mik.kabuproxy.security;

import de.mik.kabuproxy.config.KabuConfig;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Picks the authentication mechanism: the dev login when KABU_DEV_AUTH=true, Authentik OIDC otherwise.
 * (Jakarta Security 4 lets an application-provided handler choose between several mechanisms.)
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@ApplicationScoped
public class KabuAuthenticationMechanismHandler implements HttpAuthenticationMechanismHandler
{
    @Inject private KabuConfig config;

    @Inject
    @DevLogin
    private Instance<HttpAuthenticationMechanism> devMechanism;

    @Inject
    @OpenIdAuthenticationMechanismDefinition.OpenIdAuthenticationMechanism
    private Instance<HttpAuthenticationMechanism> oidcMechanism;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext context)
        throws AuthenticationException
    {
        return mechanism().validateRequest(request, response, context);
    }

    @Override
    public AuthenticationStatus secureResponse(HttpServletRequest request, HttpServletResponse response, HttpMessageContext context)
        throws AuthenticationException
    {
        return mechanism().secureResponse(request, response, context);
    }

    @Override
    public void cleanSubject(HttpServletRequest request, HttpServletResponse response, HttpMessageContext context)
    {
        mechanism().cleanSubject(request, response, context);
    }

    private HttpAuthenticationMechanism mechanism()
    {
        return config.isDevAuth() ? devMechanism.get() : oidcMechanism.get();
    }
}
