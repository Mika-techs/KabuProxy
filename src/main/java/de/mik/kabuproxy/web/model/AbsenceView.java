package de.mik.kabuproxy.web.model;

import de.mik.kabuproxy.digikabu.parser.ParsedAbsence;

public record AbsenceView(String date, String weekday, String from, String to, String remark, String kind, String excused)
{
    public boolean excusedFlag()
    {
        return excused != null && !excused.isBlank();
    }

    public boolean excusedMark()
    {
        return ParsedAbsence.EXCUSED_MARK.equals(excused);
    }
}
