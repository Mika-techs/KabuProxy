package de.mik.kabuproxy.crawler;

import de.mik.kabuproxy.persistence.entities.ChangeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares the stored lessons of one day with a freshly crawled version.
 */
public final class LessonDiff
{
    private LessonDiff()
    {
    }

    public static List<Change> compare(List<LessonSnapshot> before, List<LessonSnapshot> after)
    {
        Map<String, LessonSnapshot> old = index(before);
        Map<String, LessonSnapshot> fresh = index(after);
        List<Change> changes = new ArrayList<>();

        for (LessonSnapshot current : fresh.values())
        {
            LessonSnapshot previous = old.get(current.key());
            if (previous == null)
            {
                changes.add(new Change(ChangeType.ADDED, current.periodFrom(), current.periodTo(), null, current.describe()));
            }
            else if (!previous.sameContent(current))
            {
                changes.add(new Change(ChangeType.MODIFIED, current.periodFrom(), current.periodTo(), previous.describe(), current.describe()));
            }
        }
        for (LessonSnapshot previous : old.values())
        {
            if (!fresh.containsKey(previous.key()))
            {
                changes.add(new Change(ChangeType.REMOVED, previous.periodFrom(), previous.periodTo(), previous.describe(), null));
            }
        }
        changes.sort(Comparator.comparingInt(Change::periodFrom));
        return changes;
    }

    private static Map<String, LessonSnapshot> index(List<LessonSnapshot> lessons)
    {
        Map<String, LessonSnapshot> map = new LinkedHashMap<>();
        for (LessonSnapshot lesson : lessons)
        {
            map.putIfAbsent(lesson.key(), lesson);
        }
        return map;
    }

    public record Change(ChangeType type, int periodFrom, int periodTo, String before, String after)
    {
    }
}
