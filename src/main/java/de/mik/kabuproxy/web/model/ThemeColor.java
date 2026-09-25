package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.web.I18n;

import java.util.Arrays;
import java.util.Objects;

/**
 * The user-configurable base colours, each with a light and a dark value. The CSS derives the dependent tokens (borders,
 * muted text, soft backgrounds) from them; the defaults must match the built-ins in kabu.css.
 */
public enum ThemeColor
{
    BG("bg", "#f5f6fa", "#0f1117"),
    SURFACE("surface", "#ffffff", "#171a23"),
    TEXT("text", "#1d2130", "#e6e8ef"),
    CHANGED("changed", "#b45309", "#fbbf5c"),
    CANCEL("cancel", "#c0262d", "#ff7b7b"),
    OK("ok", "#15803d", "#5ad083"),
    HOLIDAY("holiday", "#1d6fb8", "#7cc0ff");

    private static final String LIGHT = "light-";
    private static final String DARK = "dark-";

    private final String token;
    private final String lightDefault;
    private final String darkDefault;

    ThemeColor(String token, String lightDefault, String darkDefault)
    {
        this.token = token;
        this.lightDefault = lightDefault;
        this.darkDefault = darkDefault;
    }

    public String getLabel()
    {
        return I18n.text("themeColor." + token);
    }

    /**
     * Key in {@link UserSettings#colors()} and the DB; the CSS variable is {@code --u-<key>}.
     */
    public String getLightKey()
    {
        return LIGHT + token;
    }

    public String getDarkKey()
    {
        return DARK + token;
    }

    public String getLightDefault()
    {
        return lightDefault;
    }

    public String getDarkDefault()
    {
        return darkDefault;
    }

    /**
     * Built-in value for a key, or null when the key is unknown.
     */
    public static String defaultFor(String key)
    {
        return Arrays.stream(values())
            .map(c -> c.getLightKey().equals(key) ? c.lightDefault : c.getDarkKey().equals(key) ? c.darkDefault : null)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}
