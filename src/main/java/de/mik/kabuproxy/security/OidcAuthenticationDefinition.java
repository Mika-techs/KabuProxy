package de.mik.kabuproxy.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;

/**
 * OIDC login against Authentik. The caller name is the stable {@code sub} claim; Authentik puts group names into the
 * {@code groups} claim (scope "profile"), which become the caller's roles.
 * <p>
 * Everything is EL so it is read from {@link de.mik.kabuproxy.config.KabuConfig} (env vars) at runtime.
 */
@ApplicationScoped
@OpenIdAuthenticationMechanismDefinition(
    providerURI = "#{kabuConfig.oidcProviderUri}",
    clientId = "#{kabuConfig.oidcClientId}",
    clientSecret = "#{kabuConfig.oidcClientSecret}",
    redirectURI = "#{kabuConfig.oidcRedirectUri}",
    redirectToOriginalResource = true,
    scope = {OpenIdConstant.OPENID_SCOPE, OpenIdConstant.PROFILE_SCOPE, OpenIdConstant.EMAIL_SCOPE, OpenIdConstant.OFFLINE_ACCESS_SCOPE},
    claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub", callerGroupsClaim = "groups"),
    tokenAutoRefresh = true)
public class OidcAuthenticationDefinition
{
}
