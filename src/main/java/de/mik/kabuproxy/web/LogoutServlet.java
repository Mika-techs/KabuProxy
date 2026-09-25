package de.mik.kabuproxy.web;

import de.mik.kabuproxy.config.KabuConfig;
import org.apache.logging.log4j.Logger;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Ends the local session and, with OIDC, also the Authentik session (otherwise the next request would log in silently).
 * <p>
 * The path is open (web.xml), so an already expired session does not start a login round trip first. With OIDC it must
 * not call {@code request.logout()}: TomEE's mechanism then restarts the authorization dialog (no
 * {@code LogoutDefinition}), which lands back here as {@code /logout?code=…&state=…} instead of at Authentik.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @Inject private transient Logger logger;
    @Inject private transient KabuConfig config;
    @Inject private transient Instance<OpenIdContext> openIdContext;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        if (config.isDevAuth())
        {
            request.logout();
            invalidate(request);
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        String idToken = request.getUserPrincipal() == null ? null : idToken();
        invalidate(request);
        String endSession = config.getOidcEndSessionUri();
        if (endSession == null)
        {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        response.sendRedirect(idToken == null ? endSession : endSession + "?id_token_hint=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8));
    }

    private String idToken()
    {
        try
        {
            return openIdContext.get().getIdentityToken().getToken();
        }
        catch (RuntimeException e)
        {
            logger.debug("no id token for the end-session hint: {}", e.toString());
            return null;
        }
    }

    private static void invalidate(HttpServletRequest request)
    {
        HttpSession session = request.getSession(false);
        if (session != null)
        {
            session.invalidate();
        }
    }
}
