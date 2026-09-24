package de.mik.kabuproxy.digikabu.parser;

import de.mik.kabuproxy.digikabu.DigikabuException;
import de.mik.kabuproxy.digikabu.DigikabuSession;
import de.mik.kabuproxy.persistence.entities.LessonStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the SVG timetable fragment of {@code POST /Stundenplan/StdPlanStd}.
 * <p>
 * Layout: {@code div#umgebung} holds one fixed-width svg with the period times, then one svg per weekday. Inside a
 * weekday, each lesson is a nested svg whose {@code y}/{@code height} encode the periods (row 1 starts at y=30, each
 * row is 60 high) and whose {@code x}/{@code width} encode parallel groups (e.g. x=50%, width=50% = second of two).
 */
public final class TimetableParser
{
    private static final int FIRST_ROW_Y = 30;
    private static final int ROW_HEIGHT = 60;
    private static final double FULL_WIDTH = 100.0;
    private static final Pattern DAY_HEADER = Pattern.compile("^\\p{L}{2},\\s*(\\d{1,2})\\.(\\d{1,2})\\.?$");
    private static final Pattern CLASS_HEADER = Pattern.compile("^Klasse\\s+(.+)$");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm", Locale.GERMANY);

    private TimetableParser()
    {
    }

    public static ParsedWeek parse(String html, LocalDate reference) throws DigikabuException.ParseFailed
    {
        Document doc = Jsoup.parseBodyFragment(html);
        Element container = doc.selectFirst("#umgebung");
        if (container == null)
        {
            throw new DigikabuException.ParseFailed("timetable container #umgebung missing", null, html);
        }

        List<ParsedPeriod> periods = new ArrayList<>();
        List<ParsedDay> days = new ArrayList<>();
        try
        {
            for (Element column : container.children())
            {
                if (!"svg".equals(column.normalName()))
                {
                    continue;
                }
                if (column.attr("width").endsWith("%"))
                {
                    days.add(parseDay(column, reference));
                }
                else
                {
                    periods.addAll(parsePeriods(column));
                }
            }
        }
        catch (NumberFormatException | DateTimeParseException | IllegalStateException e)
        {
            throw new DigikabuException.ParseFailed("timetable layout not understood: " + e.getMessage(), null, html);
        }

        if (days.isEmpty())
        {
            throw new DigikabuException.ParseFailed("timetable contains no weekday columns", null, html);
        }

        return new ParsedWeek(parseClassName(doc), periods, days, DigikabuSession.extractToken(html));
    }

    private static String parseClassName(Document doc)
    {
        Element heading = doc.selectFirst("#stdplanheading h3");
        if (heading == null)
        {
            return null;
        }
        Matcher matcher = CLASS_HEADER.matcher(heading.text().trim());
        return matcher.matches() ? matcher.group(1).trim() : null;
    }

    private static List<ParsedPeriod> parsePeriods(Element timeColumn)
    {
        List<ParsedPeriod> periods = new ArrayList<>();
        for (Element group : timeColumn.select("> g"))
        {
            Element rect = group.selectFirst("rect");
            Elements texts = group.select("text");
            if (rect == null || texts.size() < 3)
            {
                continue;
            }
            int period = rowOf(parseInt(rect.attr("y")));
            periods.add(new ParsedPeriod(period, LocalTime.parse(texts.get(0).text().trim(), TIME), LocalTime.parse(texts.get(2).text().trim(), TIME)));
        }
        return periods;
    }

    private static ParsedDay parseDay(Element dayColumn, LocalDate reference)
    {
        LocalDate date = null;
        for (Element text : dayColumn.select("> g > text"))
        {
            Matcher matcher = DAY_HEADER.matcher(text.text().trim());
            if (matcher.matches())
            {
                date = DateInference.resolve(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), reference);
                break;
            }
        }
        if (date == null)
        {
            throw new IllegalStateException("weekday column without date header");
        }

        List<ParsedLesson> lessons = new ArrayList<>();
        for (Element box : dayColumn.select("> svg"))
        {
            lessons.add(parseLesson(box));
        }
        return new ParsedDay(date, lessons);
    }

    private static ParsedLesson parseLesson(Element box)
    {
        int y = parseInt(box.attr("y"));
        int height = parseInt(box.attr("height"));
        int periodFrom = rowOf(y);
        int periodTo = periodFrom + Math.max(1, Math.round(height / (float) ROW_HEIGHT)) - 1;

        double x = parsePercent(box.attr("x"));
        double width = parsePercent(box.attr("width"));
        if (width <= 0.0)
        {
            width = FULL_WIDTH;
        }
        int laneCount = Math.max(1, (int) Math.round(FULL_WIDTH / width));
        int lane = Math.min(laneCount - 1, (int) Math.round(x / width));

        String teacher = null;
        String room = null;
        String subject = null;
        for (Element text : box.select("> text.sp, > text.sp_small"))
        {
            String anchor = text.attr("text-anchor");
            String value = blankToNull(text.text());
            if ("end".equals(anchor))
            {
                room = value;
            }
            else if ("middle".equals(anchor))
            {
                subject = value;
            }
            else
            {
                teacher = value;
            }
        }

        String statusRaw = null;
        for (Element rect : box.select("> rect"))
        {
            String cssClass = rect.className();
            if (!"std".equals(cssClass) && cssClass.endsWith("Std"))
            {
                statusRaw = cssClass;
                break;
            }
        }

        Element stdRect = box.selectFirst("> rect.std");
        Element hint = box.selectFirst("> text.vhinweis_vertr");
        List<String> noteLines = new ArrayList<>();
        for (Element tspan : box.select("> text.inhalt tspan"))
        {
            String line = tspan.text().trim();
            if (!line.isEmpty())
            {
                noteLines.add(line);
            }
        }

        return new ParsedLesson(
            periodFrom,
            periodTo,
            lane,
            laneCount,
            teacher,
            room,
            subject,
            statusOf(statusRaw),
            statusRaw,
            hint == null ? null : blankToNull(hint.text()),
            noteLines.isEmpty() ? null : String.join("\n", noteLines),
            stdRect == null ? null : blankToNull(stdRect.id()));
    }

    static LessonStatus statusOf(String statusRaw)
    {
        if (statusRaw == null || "regStd".equals(statusRaw))
        {
            return LessonStatus.REGULAR;
        }
        String lower = statusRaw.toLowerCase(Locale.ROOT);
        if (lower.contains("entf") || lower.contains("ausf"))
        {
            return LessonStatus.CANCELLED;
        }
        if (lower.contains("vertret"))
        {
            return LessonStatus.CHANGED;
        }
        return LessonStatus.UNKNOWN;
    }

    private static int rowOf(int y)
    {
        return Math.round((y - FIRST_ROW_Y) / (float) ROW_HEIGHT) + 1;
    }

    private static int parseInt(String value)
    {
        return (int) Math.round(Double.parseDouble(value.trim()));
    }

    private static double parsePercent(String value)
    {
        String trimmed = value.trim();
        if (trimmed.isEmpty())
        {
            return 0.0;
        }
        return Double.parseDouble(trimmed.endsWith("%") ? trimmed.substring(0, trimmed.length() - 1) : trimmed);
    }

    private static String blankToNull(String value)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
