package de.mik.kabuproxy.digikabu.parser;

import de.mik.kabuproxy.persistence.entities.DayKind;

import java.time.LocalDate;

public record ParsedCalendarDay(LocalDate date, DayKind kind, String text)
{
}
