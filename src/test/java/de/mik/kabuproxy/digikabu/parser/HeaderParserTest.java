package de.mik.kabuproxy.digikabu.parser;

import de.mik.kabuproxy.Fixtures;
import de.mik.kabuproxy.digikabu.DigikabuException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeaderParserTest
{
    @Test
    void parsesNameAndClass() throws Exception
    {
        assertEquals(new ParsedHeader("Max Muster", "WIT12A"), HeaderParser.parse(Fixtures.load("main.html")));
    }

    @Test
    void rejectsLoginPage()
    {
        assertThrows(DigikabuException.ParseFailed.class, () -> HeaderParser.parse(Fixtures.load("login_failed.html")));
    }
}
