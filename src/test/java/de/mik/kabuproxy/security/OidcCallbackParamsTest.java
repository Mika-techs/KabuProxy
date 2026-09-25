package de.mik.kabuproxy.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OidcCallbackParamsTest
{
    @Test
    void stripsOnlyCallbackParams()
    {
        assertEquals("week=2026-W39", OidcCallbackParams.strip("week=2026-W39&code=abc&state=xyz&session_state=s&iss=https%3A%2F%2Fauth"));
        assertEquals("", OidcCallbackParams.strip("code=abc&state=xyz"));
        assertEquals("", OidcCallbackParams.strip(null));
        assertEquals("statement=1", OidcCallbackParams.strip("statement=1&state=x"));
    }
}
