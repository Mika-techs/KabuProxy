package de.mik.kabuproxy.digikabu;

import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One logged-in browser-like session against digikabu.de. Not thread-safe; the timetable week is server-side
 * session state, so all calls of one session must run sequentially.
 */
public class DigikabuSession implements AutoCloseable
{
    public static final String WRONG_CREDENTIALS_MARKER = "Falscher Benutzername";

    private static final String TOKEN_FIELD = "__RequestVerificationToken";
    private static final String TIMETABLE_KIND = "StdPlanKlasse";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String USER_AGENT = "kabuProxy/0.1 (personal timetable mirror)";
    private static final int HTTP_OK = 200;
    private static final int HTTP_REDIRECT_MIN = 300;
    private static final int HTTP_REDIRECT_MAX = 399;
    private static final int HTTP_SERVER_ERROR = 500;

    private final String baseUrl;
    private final long requestDelayMillis;
    private final HttpClient client;

    @Getter private String lastUrl;
    private boolean firstRequest = true;

    public DigikabuSession(String baseUrl, long requestDelayMillis)
    {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.requestDelayMillis = requestDelayMillis;
        this.client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    }

    /**
     * Logs in. digikabu answers a successful login with a redirect, a failed one with the login page plus an error text.
     */
    public void login(String username, String password) throws DigikabuException
    {
        HttpResponse<String> loginPage = send(get("/"));
        String token = extractToken(loginPage.body());
        if (token == null)
        {
            throw new DigikabuException.ParseFailed("login page without antiforgery token", lastUrl, loginPage.body());
        }

        Map<String, String> form = new LinkedHashMap<>();
        form.put("UserName", username);
        form.put("Password", password);
        form.put(TOKEN_FIELD, token);
        HttpResponse<String> response = send(post("/Login/Proceed", form));

        if (isRedirect(response))
        {
            String location = response.headers().firstValue("Location").orElse("");
            if (location.contains("Main"))
            {
                return;
            }
            throw new DigikabuException.ParseFailed("unexpected login redirect to '" + location + "'", lastUrl, null);
        }
        if (response.statusCode() == HTTP_OK && response.body().contains(WRONG_CREDENTIALS_MARKER))
        {
            throw new DigikabuException.AuthFailed("digikabu rejected username/password");
        }
        throw new DigikabuException.ParseFailed("unexpected login response " + response.statusCode(), lastUrl, response.body());
    }

    public String fetchMainPage() throws DigikabuException
    {
        return expectPage(send(get("/Main/Index")));
    }

    public String fetchTimetable(String className) throws DigikabuException
    {
        return expectPage(send(post("/Stundenplan/StdPlanStd", Map.of("art", TIMETABLE_KIND, "bez", className))));
    }

    /**
     * Moves the server-side timetable date by {@code days} (digikabu only allows ±7 around today).
     *
     * @param token antiforgery token taken from the previously fetched timetable fragment
     */
    public void changeWeek(String className, int days, String token) throws DigikabuException
    {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("planart", TIMETABLE_KIND);
        form.put("bez", className);
        form.put("dir", Integer.toString(days));
        form.put(TOKEN_FIELD, token);
        HttpResponse<String> response = send(post("/Stundenplan/ChangeDate", form));
        if (isRedirect(response) && isLoginRedirect(response))
        {
            throw new DigikabuException.Unavailable("digikabu session expired", lastUrl, null);
        }
        if (!isRedirect(response) && response.statusCode() != HTTP_OK)
        {
            throw new DigikabuException.ParseFailed("unexpected week change response " + response.statusCode(), lastUrl, response.body());
        }
    }

    public String fetchExamPlan() throws DigikabuException
    {
        return expectPage(send(get("/SchulaufgabenPlan")));
    }

    public String fetchAbsences() throws DigikabuException
    {
        return expectPage(send(get("/Fehlzeiten")));
    }

    public static String extractToken(String html)
    {
        Element input = Jsoup.parse(html).selectFirst("input[name=" + TOKEN_FIELD + "]");
        return input == null ? null : input.attr("value");
    }

    @Override
    public void close()
    {
        client.close();
    }

    private String expectPage(HttpResponse<String> response) throws DigikabuException
    {
        if (response.statusCode() == HTTP_OK)
        {
            return response.body();
        }
        if (isRedirect(response) && isLoginRedirect(response))
        {
            throw new DigikabuException.Unavailable("digikabu session expired", lastUrl, null);
        }
        throw new DigikabuException.ParseFailed("unexpected status " + response.statusCode(), lastUrl, response.body());
    }

    private HttpRequest.Builder get(String path)
    {
        return request(path).GET();
    }

    private HttpRequest.Builder post(String path, Map<String, String> form)
    {
        String body = form.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
        return request(path)
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private HttpRequest.Builder request(String path)
    {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "de-DE,de;q=0.9");
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws DigikabuException
    {
        HttpRequest request = builder.build();
        lastUrl = request.uri().toString();
        politePause();
        HttpResponse<String> response;
        try
        {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new DigikabuException.Unavailable("digikabu not reachable: " + e.getMessage(), lastUrl, e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new DigikabuException.Unavailable("interrupted", lastUrl, e);
        }
        if (response.statusCode() >= HTTP_SERVER_ERROR)
        {
            throw new DigikabuException.Unavailable("digikabu answered " + response.statusCode(), lastUrl, null);
        }
        return response;
    }

    private void politePause()
    {
        if (firstRequest || requestDelayMillis <= 0)
        {
            firstRequest = false;
            return;
        }
        try
        {
            Thread.sleep(requestDelayMillis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isRedirect(HttpResponse<?> response)
    {
        return response.statusCode() >= HTTP_REDIRECT_MIN && response.statusCode() <= HTTP_REDIRECT_MAX;
    }

    private static boolean isLoginRedirect(HttpResponse<?> response)
    {
        String location = response.headers().firstValue("Location").orElse("");
        return location.isEmpty() || location.endsWith("/") || location.contains("Login");
    }
}
