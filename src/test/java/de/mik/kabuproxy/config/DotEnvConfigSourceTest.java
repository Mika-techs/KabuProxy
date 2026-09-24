package de.mik.kabuproxy.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotEnvConfigSourceTest
{
    @TempDir private Path dir;

    @Test
    void mapsPropertyNamesLikeEnvVars() throws Exception
    {
        Path env = dir.resolve(".env");
        Files.writeString(env, """
            # comment
            KABU_DEV_AUTH=true
            export KABU_CRED_KEY="abc=="
            KABU_DB_PASSWORD='p&ss=w#rd'
            KABU_PUBLIC_URL=
            broken line
            """);
        DotEnvConfigSource source = new DotEnvConfigSource(env);

        assertEquals("true", source.getValue("kabu.dev.auth"));
        assertEquals("abc==", source.getValue("kabu.cred.key"));
        assertEquals("p&ss=w#rd", source.getValue("KABU_DB_PASSWORD"));
        assertNull(source.getValue("kabu.public.url"));
        assertNull(source.getValue("kabu.unknown"));
        assertEquals(250, source.getOrdinal());
    }

    @Test
    void missingFileIsEmpty()
    {
        assertTrue(new DotEnvConfigSource(dir.resolve("nope")).getPropertyNames().isEmpty());
    }
}
