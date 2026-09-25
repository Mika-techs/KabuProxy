package de.mik.kabuproxy.digikabu.parser;

import java.time.LocalDate;

public record ParsedAbsence(LocalDate date, String from, String to, String remark, String kind, String excused)
{
    /** Stored as {@code excused} when digikabu only shows a check-mark icon. */
    public static final String EXCUSED_MARK = "✓";
}
