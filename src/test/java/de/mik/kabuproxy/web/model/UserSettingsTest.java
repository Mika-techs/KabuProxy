package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.persistence.entities.ThemeMode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserSettingsTest
{
    @Test
    void normalizesColors()
    {
        assertEquals("#aabbcc", UserSettings.normalizeColor(" #AABBCC "));
        assertNull(UserSettings.normalizeColor("#abc"));
        assertNull(UserSettings.normalizeColor("red"));
        assertNull(UserSettings.normalizeColor("#aabbcc; background: url(x)"));
        assertNull(UserSettings.normalizeColor(null));
    }

    @Test
    void invalidAccentFallsBackToDefault()
    {
        UserSettings settings = new UserSettings(null, "javascript:alert(1)", null);
        assertEquals(ThemeMode.SYSTEM, settings.themeMode());
        assertFalse(settings.hasAccent());
        assertEquals(UserSettings.DEFAULT_ACCENT, settings.effectiveAccent());
    }

    @Test
    void themeAttribute()
    {
        assertEquals("", UserSettings.DEFAULT.themeAttr());
        assertEquals("light", new UserSettings(ThemeMode.LIGHT, null, null).themeAttr());
        assertEquals("dark", new UserSettings(ThemeMode.DARK, null, null).themeAttr());
    }

    @Test
    void picksReadableTextOnAccent()
    {
        assertEquals("#ffffff", UserSettings.DEFAULT.accentText());
        assertEquals("#ffffff", new UserSettings(ThemeMode.SYSTEM, "#15803d", null).accentText());
        assertEquals("#1d2130", new UserSettings(ThemeMode.SYSTEM, "#facc15", null).accentText());
    }

    @Test
    void keepsOnlyKnownValidNonDefaultColors()
    {
        UserSettings settings = new UserSettings(ThemeMode.SYSTEM, null, Map.of(
            "dark-bg", "#000000",
            "light-bg", "#F5F6FA",
            "light-surface", "red; background: url(x)",
            "evil", "#123456"));
        assertEquals(Map.of("dark-bg", "#000000"), settings.colors());
        assertEquals("#000000", settings.color("dark-bg"));
        assertEquals(ThemeColor.BG.getLightDefault(), settings.color("light-bg"));
    }

    @Test
    void buildsInlineStyle()
    {
        assertEquals("", UserSettings.DEFAULT.style());
        UserSettings settings = new UserSettings(ThemeMode.SYSTEM, "#15803d", Map.of("light-text", "#000000", "dark-bg", "#101010"));
        assertEquals("--user-accent: #15803d; --user-accent-text: #ffffff; --u-dark-bg: #101010; --u-light-text: #000000;", settings.style());
        assertEquals("--u-dark-bg: #101010;", new UserSettings(null, null, Map.of("dark-bg", "#101010")).style());
    }

    @Test
    void keepsColorsWhenSwitchingMode()
    {
        UserSettings settings = new UserSettings(ThemeMode.SYSTEM, "#15803d", Map.of("dark-bg", "#101010")).withThemeMode(ThemeMode.DARK);
        assertEquals(ThemeMode.DARK, settings.themeMode());
        assertEquals("#15803d", settings.accentColor());
        assertEquals(Map.of("dark-bg", "#101010"), settings.colors());
    }
}
