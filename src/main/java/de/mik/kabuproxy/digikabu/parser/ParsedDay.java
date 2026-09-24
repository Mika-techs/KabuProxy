package de.mik.kabuproxy.digikabu.parser;

import java.time.LocalDate;
import java.util.List;

public record ParsedDay(LocalDate date, List<ParsedLesson> lessons)
{
}
