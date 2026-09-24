package de.mik.kabuproxy.digikabu.parser;

import java.time.LocalTime;

public record ParsedPeriod(int period, LocalTime start, LocalTime end)
{
}
