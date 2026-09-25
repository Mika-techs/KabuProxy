package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.persistence.entities.ChangeType;
import de.mik.kabuproxy.web.I18n;

import java.util.Locale;

public record ChangeView(String dayLabel, String periodLabel, ChangeType type, String before, String after, String detectedLabel)
{
    public String typeLabel()
    {
        return I18n.text("change." + type.name());
    }

    public String cssClass()
    {
        return "change change--" + type.name().toLowerCase(Locale.ROOT);
    }
}
