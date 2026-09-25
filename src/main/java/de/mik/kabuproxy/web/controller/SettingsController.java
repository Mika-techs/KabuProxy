package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.persistence.entities.LessonColorKey;
import de.mik.kabuproxy.persistence.entities.ThemeMode;
import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.AccountService;
import de.mik.kabuproxy.service.SettingsService;
import de.mik.kabuproxy.service.TimetableQueryService;
import de.mik.kabuproxy.web.I18n;
import de.mik.kabuproxy.web.model.ThemeColor;
import de.mik.kabuproxy.web.model.UserSettings;
import lombok.Getter;
import lombok.Setter;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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
        new AccentPreset("indigo", UserSettings.DEFAULT_ACCENT),
        new AccentPreset("blue", "#2563eb"),
        new AccentPreset("petrol", "#0e7490"),
        new AccentPreset("green", "#15803d"),
        new AccentPreset("orange", "#c2410c"),
        new AccentPreset("red", "#b91c1c"),
        new AccentPreset("pink", "#be185d"),
        new AccentPreset("violet", "#7c3aed"),
        new AccentPreset("slate", "#475569"));

    @Inject private transient SettingsService settingsService;
    @Inject private transient AccountService accountService;
    @Inject private transient TimetableQueryService queryService;
    @Inject private UserSession userSession;

    @Getter @Setter private String themeMode;
    @Getter @Setter private String accentColor;
    /**
     * Picker values for every {@link ThemeColor} key; built-in values are dropped again on save.
     */
    @Getter private final Map<String, String> colors = new HashMap<>();
    /**
     * One row per subject, followed by one per teacher when the subject has several (or a teacher colour is stored).
     */
    @Getter private List<LessonColorRow> lessonRows = List.of();

    @PostConstruct
    void init()
    {
        UserSettings settings = settingsService.load(userSession.getUserId());
        themeMode = settings.themeMode().name();
        accentColor = settings.accentColor();
        fillColors(settings);
        lessonRows = buildLessonRows(settings);
        fillLessonColors(settings);
    }

    private List<LessonColorRow> buildLessonRows(UserSettings settings)
    {
        Map<String, TreeSet<String>> teachers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        accountService.findByUser(userSession.getUserId())
            .filter(account -> account.classId() != null)
            .ifPresent(account -> queryService.subjectTeachers(account.classId())
                .forEach((subject, names) -> teachers.put(subject, teacherSet(names))));
        Map<String, TreeSet<String>> storedTeachers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (LessonColorKey key : settings.lessonColors().keySet())
        {
            teachers.computeIfAbsent(key.getSubject(), k -> teacherSet(List.of()));
            if (!key.isWholeSubject())
            {
                storedTeachers.computeIfAbsent(key.getSubject(), k -> teacherSet(List.of())).add(key.getTeacher());
            }
        }
        List<LessonColorRow> rows = new ArrayList<>();
        teachers.forEach((subject, names) ->
        {
            rows.add(new LessonColorRow(subject, ""));
            TreeSet<String> stored = storedTeachers.getOrDefault(subject, teacherSet(List.of()));
            // a single teacher needs no own row - unless a colour for them is still stored
            if (names.size() > 1 || !stored.isEmpty())
            {
                TreeSet<String> all = teacherSet(names);
                all.addAll(stored);
                all.forEach(teacher -> rows.add(new LessonColorRow(subject, teacher)));
            }
        });
        return List.copyOf(rows);
    }

    private static TreeSet<String> teacherSet(Collection<String> names)
    {
        TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(names);
        return set;
    }

    private void fillLessonColors(UserSettings settings)
    {
        lessonRows.forEach(row -> row.setColor(settings.lessonColors().getOrDefault(row.key(), "")));
    }

    /**
     * Colour a row shows: its own, for a teacher row else the subject's; null follows the accent.
     */
    private String effectiveColor(LessonColorRow row)
    {
        String own = UserSettings.normalizeColor(row.getColor());
        if (own != null || !row.isTeacherRow())
        {
            return own;
        }
        return lessonRows.stream()
            .filter(r -> !r.isTeacherRow() && r.getSubject().equals(row.getSubject()))
            .findFirst()
            .map(r -> UserSettings.normalizeColor(r.getColor()))
            .orElse(null);
    }

    /**
     * Preview style of a row in the lesson colour table, same property as on the timetable.
     */
    public String lessonStyle(LessonColorRow row)
    {
        return UserSettings.lessonColorStyle(effectiveColor(row));
    }

    public String lessonPickerColor(LessonColorRow row)
    {
        String color = effectiveColor(row);
        return color == null ? getPickerColor() : color;
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
                Messages.error("settings.invalidColor");
                return;
            }
        }
        // the built-in accent has tuned dark-mode values, so store it as "default"
        if (UserSettings.DEFAULT_ACCENT.equals(color))
        {
            color = null;
        }
        Map<LessonColorKey, String> lessonColors = new HashMap<>();
        lessonRows.forEach(row -> lessonColors.put(row.key(), row.getColor()));
        UserSettings settings = new UserSettings(mode, color, colors, lessonColors);
        settingsService.save(userSession.getUserId(), settings);
        themeMode = mode.name();
        accentColor = color;
        fillColors(settings);
        fillLessonColors(settings);
        Messages.info("settings.saved");
    }

    public void reset()
    {
        accentColor = null;
        themeMode = ThemeMode.SYSTEM.name();
        fillColors(UserSettings.DEFAULT);
        fillLessonColors(UserSettings.DEFAULT);
        save();
    }

    /**
     * A row of the lesson colour table; {@code color} is the hidden field: {@code #rrggbb}, or empty to follow the
     * subject's colour (teacher row) or the accent (subject row). A class, not a record: JSF writes {@code color} back.
     */
    @Getter
    public static class LessonColorRow implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private final String subject;
        /**
         * Empty for the row of the whole subject.
         */
        private final String teacher;
        @Setter private String color = "";

        LessonColorRow(String subject, String teacher)
        {
            this.subject = subject;
            this.teacher = teacher;
        }

        public boolean isTeacherRow()
        {
            return !teacher.isEmpty();
        }

        LessonColorKey key()
        {
            return new LessonColorKey(subject, teacher);
        }
    }

    /**
     * @param name key suffix of the label in the {@link I18n} bundle
     */
    public record AccentPreset(String name, String color)
    {
        public String label()
        {
            return I18n.text("accent." + name);
        }
    }
}
