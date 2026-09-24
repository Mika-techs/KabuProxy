package de.mik.kabuproxy.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * OIDC redirect URI. The authentication mechanism consumes the code before this runs and normally redirects to the
 * originally requested page itself; this is only the fallback.
 */
@WebServlet("/callback")
public class CallbackServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.sendRedirect(request.getContextPath() + "/");
    }
}
