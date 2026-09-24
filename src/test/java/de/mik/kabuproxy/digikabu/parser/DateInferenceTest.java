package de.mik.kabuproxy.digikabu.parser;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateInferenceTest
{
    @Test
    void picksClosestYear()
    {
        assertEquals(LocalDate.of(2026, 9, 21), DateInference.resolve(21, 9, LocalDate.of(2026, 9, 24)));
        assertEquals(LocalDate.of(2027, 1, 2), DateInference.resolve(2, 1, LocalDate.of(2026, 12, 28)));
        assertEquals(LocalDate.of(2026, 12, 28), DateInference.resolve(28, 12, LocalDate.of(2027, 1, 3)));
        assertEquals(LocalDate.of(2028, 2, 29), DateInference.resolve(29, 2, LocalDate.of(2027, 6, 1)));
    }
}
