package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.persistence.entities.ChangeType;

import java.util.Locale;

public record ChangeView(String dayLabel, String periodLabel, ChangeType type, String before, String after, String detectedLabel)
{
    public String typeLabel()
    {
        return switch (type)
        {
            case ADDED -> "neu";
            case REMOVED -> "entfällt";
            case MODIFIED -> "geändert";
        };
    }

    public String cssClass()
    {
        return "change change--" + type.name().toLowerCase(Locale.ROOT);
    }
}
