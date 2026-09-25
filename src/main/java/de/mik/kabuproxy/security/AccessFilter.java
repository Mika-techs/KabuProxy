package de.mik.kabuproxy.security;

import de.mik.kabuproxy.config.KabuConfig;
import de.mik.kabuproxy.persistence.entities.AppUserEntity;
import de.mik.kabuproxy.persistence.entities.UserStatus;
import de.mik.kabuproxy.service.UserService;
import org.apache.logging.log4j.Logger;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

/**
 * Runs after the container authenticated the caller (Authentik or dev login):
 * registers unknown users as PENDING, keeps pending/disabled users on the waiting page (pending users may still link their
 * digikabu account on the settings page) and guards the admin page.
 */
@WebFilter(filterName = "AccessFilter", urlPatterns = "/*")
public class AccessFilter extends HttpFilter
{
    private static final long serialVersionUID = 1L;

    private static final String PENDING_PAGE = "/pending.xhtml";
    private static final String SETTINGS_PAGE = "/einstellungen.xhtml";
    private static final String ADMIN_PAGE = "/admin.xhtml";
    private static final String THEME_SERVLET = "/theme";
    private static final List<String> OPEN_PREFIXES = List.of("/health", "/callback", "/logout", "/jakarta.faces.resource/", "/resources/",
        "/favicon");

    @Inject private Logger logger;
    @Inject private KabuConfig config;
    @Inject private UserService userService;
    @Inject private UserSession userSession;
    @Inject private Instance<OpenIdContext> openIdContext;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Principal principal = request.getUserPrincipal();
        if (principal == null || OPEN_PREFIXES.stream().anyMatch(path::startsWith))
        {
            chain.doFilter(request, response);
            return;
        }

        boolean admin = request.isUserInRole(config.getAdminGroup());
        if (!userSession.isLoggedIn())
        {
            register(principal.getName(), admin);
        }
        userSession.setAdmin(admin);

        UserStatus status = userService.status(userSession.getUserId());
        if (status == null)
        {
            // user was deleted by an admin while logged in
            request.getSession().invalidate();
            response.sendRedirect(request.getContextPath() + "/logout");
            return;
        }
        userSession.setActive(status == UserStatus.ACTIVE);

        if (ADMIN_PAGE.equals(path) && !admin)
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        boolean pending = status == UserStatus.PENDING && !admin;
        userSession.setPending(pending);
        boolean allowed = PENDING_PAGE.equals(path) || THEME_SERVLET.equals(path) || (pending && SETTINGS_PAGE.equals(path));
        if ((status == UserStatus.DISABLED || pending) && !allowed)
        {
            response.sendRedirect(request.getContextPath() + PENDING_PAGE);
            return;
        }
        chain.doFilter(request, response);
    }

    private void register(String subject, boolean admin)
    {
        String username = subject;
        String email = null;
        if (!config.isDevAuth())
        {
            try
            {
                OpenIdContext context = openIdContext.get();
                username = context.getClaims().getPreferredUsername().orElse(subject);
                email = context.getClaims().getEmail().orElse(null);
            }
            catch (RuntimeException e)
            {
                logger.warn("could not read OIDC claims for {}: {}", subject, e.toString());
            }
        }
        AppUserEntity user = userService.registerLogin(subject, username, email, admin);
        userSession.setUserId(user.getId());
        userSession.setUsername(user.getUsername());
    }
}
