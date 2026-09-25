package de.mik.kabuproxy.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The query parameters Authentik appends to the redirect URI. With {@code redirectToOriginalResource} TomEE forwards them
 * to the original page, so they would stay in the address bar – and a reload after a restart (session and stored state
 * gone) fails the login. These helpers detect and strip them.
 */
public final class OidcCallbackParams
{
    private static final Set<String> PARAMS = Set.of("code", "state", "session_state", "iss");

    private OidcCallbackParams()
    {
    }

    /**
     * @return whether the request carries an OIDC callback (a {@code state} parameter)
     */
    public static boolean present(HttpServletRequest request)
    {
        return "GET".equals(request.getMethod()) && request.getParameter("state") != null;
    }

    /**
     * @return the request URL (context-relative path kept) without the callback parameters; the callback URI itself maps
     *     to the context root
     */
    public static String strippedUrl(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        if ((request.getContextPath() + "/callback").equals(uri))
        {
            return request.getContextPath() + "/";
        }
        String query = strip(request.getQueryString());
        return query.isEmpty() ? uri : uri + "?" + query;
    }

    static String strip(String query)
    {
        if (query == null || query.isEmpty())
        {
            return "";
        }
        return Arrays.stream(query.split("&"))
            .filter(pair -> !pair.isEmpty() && !PARAMS.contains(pair.split("=", 2)[0]))
            .collect(Collectors.joining("&"));
    }
}
