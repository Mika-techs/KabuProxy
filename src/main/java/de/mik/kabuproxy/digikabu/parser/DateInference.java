package de.mik.kabuproxy.digikabu.parser;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * digikabu often prints "Mo, 21.09." without a year; pick the year that puts the date closest to a reference day.
 */
public final class DateInference
{
    private DateInference()
    {
    }

    public static LocalDate resolve(int day, int month, LocalDate reference)
    {
        LocalDate best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int year = reference.getYear() - 1; year <= reference.getYear() + 1; year++)
        {
            LocalDate candidate;
            try
            {
                candidate = LocalDate.of(year, month, day);
            }
            catch (DateTimeException e)
            {
                continue;
            }
            long distance = Math.abs(ChronoUnit.DAYS.between(reference, candidate));
            if (distance < bestDistance)
            {
                best = candidate;
                bestDistance = distance;
            }
        }
        if (best == null)
        {
            throw new DateTimeException("invalid day/month " + day + "." + month + ".");
        }
        return best;
    }
}
