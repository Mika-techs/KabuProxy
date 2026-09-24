package de.mik.kabuproxy.web;

import javax.sql.DataSource;

import jakarta.annotation.Resource;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Unauthenticated liveness/readiness probe for the container healthcheck: 200 when the DB answers.
 */
@WebServlet("/health")
public class HealthServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final int DB_TIMEOUT_SECONDS = 3;

    @Resource(name = "KabuDataSource")
    private transient DataSource dataSource;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.setContentType("text/plain");
        try (Connection connection = dataSource.getConnection())
        {
            if (connection.isValid(DB_TIMEOUT_SECONDS))
            {
                response.getWriter().write("UP");
                return;
            }
        }
        catch (SQLException e)
        {
            // reported as DOWN below
        }
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.getWriter().write("DOWN");
    }
}
