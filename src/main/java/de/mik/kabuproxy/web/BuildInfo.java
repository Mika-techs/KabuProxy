package de.mik.kabuproxy.web;

import org.apache.logging.log4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * When the running image was built. The stamp versions static resource URLs (see {@link VersionedResourceHandler}),
 * so browsers fetch new css/js after every deployment.
 */
@ApplicationScoped
public class BuildInfo
{
    /** written by the Dockerfile at image build time (UTC, ISO-8601); missing in dev runs */
    private static final String BUILD_TIME_RESOURCE = "/WEB-INF/build-time";

    @Inject private Logger logger;
    @Inject private ServletContext servletContext;

    private Optional<Instant> buildTime;
    private String stamp;

    @PostConstruct
    void init()
    {
        buildTime = readBuildTime();
        // dev runs have no build-time file: every restart counts as a new build
        stamp = Long.toString(buildTime.orElseGet(Instant::now).getEpochSecond());
    }

    public Optional<Instant> buildTime()
    {
        return buildTime;
    }

    public String stamp()
    {
        return stamp;
    }

    private Optional<Instant> readBuildTime()
    {
        try (InputStream in = servletContext.getResourceAsStream(BUILD_TIME_RESOURCE))
        {
            if (in == null)
            {
                return Optional.empty();
            }
            return Optional.of(Instant.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim()));
        }
        catch (IOException | DateTimeParseException e)
        {
            logger.warn("cannot read {}: {}", BUILD_TIME_RESOURCE, e.getMessage());
            return Optional.empty();
        }
    }
}
