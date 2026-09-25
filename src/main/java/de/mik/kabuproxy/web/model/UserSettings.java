package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.persistence.entities.LessonColorKey;
import de.mik.kabuproxy.persistence.entities.ThemeMode;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * A user's UI preferences. {@code accentColor} is either null (built-in accent) or a normalized {@code #rrggbb};
 * {@code colors} maps {@link ThemeColor} keys to normalized colours and only holds the ones that differ from the built-ins;
 * {@code lessonColors} maps a subject, or a subject taught by one teacher, to a normalized colour (see {@link #lessonColor}).
 */
public record UserSettings(ThemeMode themeMode, String accentColor, Map<String, String> colors, Map<LessonColorKey, String> lessonColors)
{
    public static final UserSettings DEFAULT = new UserSettings(ThemeMode.SYSTEM, null, Map.of(), Map.of());
    public static final String DEFAULT_ACCENT = "#4f46e5";

    /**
     * Length of {@code lesson.subject}/{@code lesson.teacher} and the matching {@code user_lesson_color} columns.
     */
    private static final int MAX_KEY_LENGTH = 100;
    private static final int MAX_LESSON_COLORS = 200;
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-f]{6}");

    public UserSettings
    {
        themeMode = themeMode == null ? ThemeMode.SYSTEM : themeMode;
        accentColor = normalizeColor(accentColor);
        colors = sanitize(colors);
        lessonColors = sanitizeLessonColors(lessonColors);
    }

    /**
     * Strict {@code #rrggbb} check – the value ends up in an inline style, so nothing else may pass.
     */
    public static String normalizeColor(String color)
    {
        if (color == null)
        {
            return null;
        }
        String normalized = color.trim().toLowerCase(Locale.ROOT);
        return HEX_COLOR.matcher(normalized).matches() ? normalized : null;
    }

    /**
     * Keeps known keys with valid colours that differ from the built-in value (sorted, so the style is stable).
     */
    private static Map<String, String> sanitize(Map<String, String> colors)
    {
        Map<String, String> clean = new TreeMap<>();
        if (colors != null)
        {
            colors.forEach((key, value) ->
            {
                String builtIn = ThemeColor.defaultFor(key);
                String color = normalizeColor(value);
                if (builtIn != null && color != null && !color.equals(builtIn))
                {
                    clean.put(key, color);
                }
            });
        }
        return Collections.unmodifiableMap(clean);
    }

    /**
     * Keeps valid colours of non-blank subjects (keys are trimmed, as stored in {@code lesson}); bounded because the keys
     * come from a form.
     */
    private static Map<LessonColorKey, String> sanitizeLessonColors(Map<LessonColorKey, String> lessonColors)
    {
        Map<LessonColorKey, String> clean = new TreeMap<>();
        if (lessonColors != null)
        {
            lessonColors.forEach((key, value) ->
            {
                String color = normalizeColor(value);
                if (key != null && !key.getSubject().isEmpty() && key.getSubject().length() <= MAX_KEY_LENGTH
                    && key.getTeacher().length() <= MAX_KEY_LENGTH && color != null && clean.size() < MAX_LESSON_COLORS)
                {
                    clean.put(new LessonColorKey(key.getSubject(), key.getTeacher()), color);
                }
            });
        }
        return Collections.unmodifiableMap(clean);
    }

    public UserSettings withThemeMode(ThemeMode mode)
    {
        return new UserSettings(mode, accentColor, colors, lessonColors);
    }

    /**
     * Value of the {@code data-theme} attribute on {@code <html>}; empty follows the OS.
     */
    public String themeAttr()
    {
        return switch (themeMode)
        {
            case LIGHT -> "light";
            case DARK -> "dark";
            case SYSTEM -> "";
        };
    }

    public boolean hasAccent()
    {
        return accentColor != null;
    }

    public String effectiveAccent()
    {
        return hasAccent() ? accentColor : DEFAULT_ACCENT;
    }

    /**
     * The user's colour for a {@link ThemeColor} key, falling back to the built-in one.
     */
    public String color(String key)
    {
        return colors.getOrDefault(key, ThemeColor.defaultFor(key));
    }

    /**
     * The user's colour for a lesson: the one for this subject and teacher, else the subject's, else null (accent).
     */
    public String lessonColor(String subject, String teacher)
    {
        if (subject == null)
        {
            return null;
        }
        String color = teacher == null || teacher.isBlank() ? null : lessonColors.get(new LessonColorKey(subject, teacher));
        return color != null ? color : lessonColors.get(LessonColorKey.of(subject));
    }

    /**
     * Inline style for a lesson: {@code --lesson-color}, which kabu.css turns into the side bar (lightened in dark mode);
     * empty when the lesson follows the accent.
     */
    public String lessonStyle(String subject, String teacher)
    {
        return lessonColorStyle(lessonColor(subject, teacher));
    }

    /**
     * {@code --lesson-color} for a colour; empty when it is null or not a strict {@code #rrggbb}.
     */
    public static String lessonColorStyle(String color)
    {
        String normalized = normalizeColor(color);
        return normalized == null ? "" : "--lesson-color: " + normalized + ";";
    }

    /**
     * Inline style for {@code <html>}: the custom properties kabu.css picks up. Only validated keys and colours get here.
     */
    public String style()
    {
        StringBuilder style = new StringBuilder();
        if (hasAccent())
        {
            style.append("--user-accent: ").append(accentColor).append("; --user-accent-text: ").append(accentText()).append(';');
        }
        colors.forEach((key, color) -> style.append(style.isEmpty() ? "" : " ").append("--u-").append(key).append(": ").append(color).append(';'));
        return style.toString();
    }

    /**
     * Readable text colour on top of the accent in light mode (WCAG relative luminance).
     */
    public String accentText()
    {
        String hex = effectiveAccent();
        double luminance = 0.2126 * channel(hex, 1) + 0.7152 * channel(hex, 3) + 0.0722 * channel(hex, 5);
        return luminance > 0.4 ? "#1d2130" : "#ffffff";
    }

    private static double channel(String hex, int offset)
    {
        double c = Integer.parseInt(hex.substring(offset, offset + 2), 16) / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
