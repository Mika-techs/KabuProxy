package de.mik.kabuproxy.digikabu.parser;

import java.util.List;

public record ParsedAbsences(int fullDays, int fullDaysUnexcused, int hours, int hoursUnexcused, List<ParsedAbsence> entries)
{
}
