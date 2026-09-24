package de.mik.kabuproxy.web;

import de.mik.kabuproxy.persistence.entities.ThemeMode;
import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.SettingsService;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * Stores the light/dark choice of the topbar toggle (POST mode=light|dark|system, called by kabu.js).
 * The custom header can't be sent cross-site without a CORS preflight, which keeps foreign pages out.
 */
@WebServlet("/theme")
public class ThemeServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private static final String REQUIRED_HEADER = "X-Kabu-Theme";

    @Inject private transient UserSession userSession;
    @Inject private transient SettingsService settingsService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if (request.getHeader(REQUIRED_HEADER) == null || !userSession.isLoggedIn())
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        ThemeMode mode;
        try
        {
            mode = ThemeMode.valueOf(String.valueOf(request.getParameter("mode")).toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e)
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        settingsService.saveThemeMode(userSession.getUserId(), mode);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
