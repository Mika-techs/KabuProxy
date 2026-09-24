package de.mik.kabuproxy.web;

import de.mik.kabuproxy.config.KabuConfig;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Ends the local session and, with OIDC, also the Authentik session (otherwise the next request would log in silently).
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @Inject private transient KabuConfig config;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        request.logout();
        HttpSession session = request.getSession(false);
        if (session != null)
        {
            session.invalidate();
        }
        String endSession = config.getOidcEndSessionUri();
        if (config.isDevAuth() || endSession == null)
        {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        response.sendRedirect(endSession);
    }
}
