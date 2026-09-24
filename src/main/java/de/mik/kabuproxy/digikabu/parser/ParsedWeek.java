package de.mik.kabuproxy.digikabu.parser;

import java.util.List;

/**
 * @param token antiforgery token of the week navigation form, needed for the next ChangeDate call
 */
public record ParsedWeek(String className, List<ParsedPeriod> periods, List<ParsedDay> days, String token)
{
}
