package de.mik.kabuproxy.web.model;

public record AbsenceView(String date, String weekday, String from, String to, String remark, String kind, String excused)
{
    public boolean excusedFlag()
    {
        return excused != null && !excused.isBlank();
    }
}
