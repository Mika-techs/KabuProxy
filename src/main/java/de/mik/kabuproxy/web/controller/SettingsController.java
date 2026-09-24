package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.persistence.entities.ThemeMode;
import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.SettingsService;
import de.mik.kabuproxy.web.model.ThemeColor;
import de.mik.kabuproxy.web.model.UserSettings;
import lombok.Getter;
import lombok.Setter;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The user's own settings page.
 */
@Named
@ViewScoped
public class SettingsController implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Preset accents; all readable on the light and (lightened) on the dark background.
     */
    private static final List<AccentPreset> PRESETS = List.of(
        new AccentPreset("Indigo", UserSettings.DEFAULT_ACCENT),
        new AccentPreset("Blau", "#2563eb"),
        new AccentPreset("Petrol", "#0e7490"),
        new AccentPreset("Grün", "#15803d"),
        new AccentPreset("Orange", "#c2410c"),
        new AccentPreset("Rot", "#b91c1c"),
        new AccentPreset("Pink", "#be185d"),
        new AccentPreset("Violett", "#7c3aed"),
        new AccentPreset("Schiefer", "#475569"));

    @Inject private transient SettingsService settingsService;
    @Inject private UserSession userSession;

    @Getter @Setter private String themeMode;
    @Getter @Setter private String accentColor;
    /**
     * Picker values for every {@link ThemeColor} key; built-in values are dropped again on save.
     */
    @Getter private final Map<String, String> colors = new HashMap<>();

    @PostConstruct
    void init()
    {
        UserSettings settings = settingsService.load(userSession.getUserId());
        themeMode = settings.themeMode().name();
        accentColor = settings.accentColor();
        fillColors(settings);
    }

    private void fillColors(UserSettings settings)
    {
        colors.clear();
        for (ThemeColor color : ThemeColor.values())
        {
            colors.put(color.getLightKey(), settings.color(color.getLightKey()));
            colors.put(color.getDarkKey(), settings.color(color.getDarkKey()));
        }
    }

    public List<ThemeColor> getThemeColors()
    {
        return List.of(ThemeColor.values());
    }

    public List<AccentPreset> getPresets()
    {
        return PRESETS;
    }

    public String getPickerColor()
    {
        return accentColor == null ? UserSettings.DEFAULT_ACCENT : accentColor;
    }

    public void save()
    {
        ThemeMode mode;
        try
        {
            mode = ThemeMode.valueOf(themeMode);
        }
        catch (IllegalArgumentException | NullPointerException e)
        {
            mode = ThemeMode.SYSTEM;
        }
        String color = null;
        if (accentColor != null && !accentColor.isBlank())
        {
            color = UserSettings.normalizeColor(accentColor);
            if (color == null)
            {
                Messages.error("Ungültige Farbe – bitte im Format #rrggbb angeben.");
                return;
            }
        }
        // the built-in accent has tuned dark-mode values, so store it as "default"
        if (UserSettings.DEFAULT_ACCENT.equals(color))
        {
            color = null;
        }
        UserSettings settings = new UserSettings(mode, color, colors);
        settingsService.save(userSession.getUserId(), settings);
        themeMode = mode.name();
        accentColor = color;
        fillColors(settings);
        Messages.info("Einstellungen gespeichert.");
    }

    public void reset()
    {
        accentColor = null;
        themeMode = ThemeMode.SYSTEM.name();
        fillColors(UserSettings.DEFAULT);
        save();
    }

    public record AccentPreset(String label, String color)
    {
    }
}
