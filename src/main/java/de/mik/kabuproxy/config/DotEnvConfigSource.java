package de.mik.kabuproxy.config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Local-dev convenience: reads {@code ./.env} (or {@code -Dkabu.dotenv=/path}) so an IDE run needs no env vars.
 * Ordinal 250 = below real environment variables (300), above microprofile-config.properties (100), so a deployment
 * always wins. In the container there is no .env in the working directory and this source is empty.
 */
public class DotEnvConfigSource implements ConfigSource
{
    private static final int ORDINAL = 250;

    private final Map<String, String> values;

    public DotEnvConfigSource()
    {
        this(Path.of(System.getProperty("kabu.dotenv", System.getProperty("user.dir") + "/.env")));
    }

    DotEnvConfigSource(Path file)
    {
        this.values = Files.isRegularFile(file) ? parse(file) : Map.of();
    }

    @Override
    public Set<String> getPropertyNames()
    {
        return Collections.unmodifiableSet(values.keySet());
    }

    /**
     * Same lookup rules as the MicroProfile env var source: {@code kabu.dev.auth} finds {@code KABU_DEV_AUTH}.
     */
    @Override
    public String getValue(String propertyName)
    {
        String value = values.get(propertyName);
        if (value == null)
        {
            String sanitized = propertyName.replaceAll("[^A-Za-z0-9]", "_");
            value = values.getOrDefault(sanitized, values.get(sanitized.toUpperCase(Locale.ROOT)));
        }
        return value == null || value.isEmpty() ? null : value;
    }

    @Override
    public String getName()
    {
        return "dotenv";
    }

    @Override
    public int getOrdinal()
    {
        return ORDINAL;
    }

    static Map<String, String> parse(Path file)
    {
        Map<String, String> result = new LinkedHashMap<>();
        try
        {
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8))
            {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("="))
                {
                    continue;
                }
                if (line.startsWith("export "))
                {
                    line = line.substring("export ".length()).strip();
                }
                int eq = line.indexOf('=');
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"") || value.startsWith("'") && value.endsWith("'")))
                {
                    value = value.substring(1, value.length() - 1);
                }
                result.put(key, value);
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot read " + file, e);
        }
        return result;
    }
}
