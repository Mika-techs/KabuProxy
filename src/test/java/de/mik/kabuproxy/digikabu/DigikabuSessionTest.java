package de.mik.kabuproxy.digikabu;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.mik.kabuproxy.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@link DigikabuSession} against an in-process fake of digikabu's login/session behaviour.
 */
class DigikabuSessionTest
{
    private static final String SESSION_COOKIE = ".AspNetCore.Session=abc";
    private static final String LOGIN_PAGE = "<form method='post' action='/Login/Proceed'>"
        + "<input name='__RequestVerificationToken' type='hidden' value='tok123' /></form>";

    private final List<String> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private String baseUrl;
    private volatile boolean serverDown;

    @BeforeEach
    void start() throws IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop()
    {
        server.stop(0);
    }

    @Test
    void logsInAndFetchesWithSessionCookie() throws Exception
    {
        try (DigikabuSession session = new DigikabuSession(baseUrl, 0))
        {
            session.login("muster1", "richtig&geheim");
            assertTrue(session.fetchMainPage().contains("Max Muster"));
            assertTrue(session.fetchAbsences().contains("Fehlzeiten"));
        }
        assertTrue(requests.contains("POST /Login/Proceed UserName=muster1&Password=richtig&geheim&__RequestVerificationToken=tok123"));
    }

    @Test
    void wrongPasswordIsAuthFailure()
    {
        try (DigikabuSession session = new DigikabuSession(baseUrl, 0))
        {
            assertThrows(DigikabuException.AuthFailed.class, () -> session.login("muster1", "falsch"));
        }
    }

    @Test
    void missingSessionIsUnavailable()
    {
        try (DigikabuSession session = new DigikabuSession(baseUrl, 0))
        {
            assertThrows(DigikabuException.Unavailable.class, session::fetchMainPage);
        }
    }

    @Test
    void serverErrorIsUnavailable()
    {
        serverDown = true;
        try (DigikabuSession session = new DigikabuSession(baseUrl, 0))
        {
            assertThrows(DigikabuException.Unavailable.class, () -> session.login("muster1", "richtig&geheim"));
        }
    }

    @Test
    void unreachableHostIsUnavailable()
    {
        server.stop(0);
        try (DigikabuSession session = new DigikabuSession(baseUrl, 0))
        {
            assertThrows(DigikabuException.Unavailable.class, () -> session.login("muster1", "x"));
        }
    }

    private void handle(HttpExchange exchange) throws IOException
    {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String path = exchange.getRequestURI().getPath();
        requests.add(exchange.getRequestMethod() + " " + path + (body.isEmpty() ? "" : " " + URLDecoder.decode(body, StandardCharsets.UTF_8)));
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        boolean loggedIn = cookie != null && cookie.contains(SESSION_COOKIE);

        if (serverDown)
        {
            respond(exchange, 503, "down");
        }
        else if ("/".equals(path))
        {
            respond(exchange, 200, LOGIN_PAGE);
        }
        else if ("/Login/Proceed".equals(path))
        {
            Map<String, String> form = parseForm(body);
            if ("richtig&geheim".equals(form.get("Password")) && "tok123".equals(form.get("__RequestVerificationToken")))
            {
                exchange.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE + "; path=/; httponly");
                redirect(exchange, "/Main/TestRedirect");
            }
            else
            {
                respond(exchange, 200, Fixtures.load("login_failed.html"));
            }
        }
        else if (!loggedIn)
        {
            redirect(exchange, baseUrl + "/");
        }
        else if ("/Main/Index".equals(path))
        {
            respond(exchange, 200, Fixtures.load("main.html"));
        }
        else if ("/Fehlzeiten".equals(path))
        {
            respond(exchange, 200, Fixtures.load("absences.html"));
        }
        else
        {
            respond(exchange, 404, "not found");
        }
    }

    private static Map<String, String> parseForm(String body)
    {
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&"))
        {
            String[] kv = pair.split("=", 2);
            form.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return form;
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException
    {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException
    {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
