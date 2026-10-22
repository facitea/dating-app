package com.dating.backend.matching;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class DailyMatchScheduleTest {
    private DailyMatchSchedule.Window at(String local) {
        return DailyMatchSchedule.at(LocalDateTime.parse(local).atZone(ZoneId.of("Asia/Seoul")).toInstant());
    }
    @Test void eightAmBeginsNewDayButMidnightDoesNot() {
        assertEquals(LocalDate.of(2026, 10, 20), at("2026-10-21T07:59:59").day());
        assertEquals(LocalDate.of(2026, 10, 21), at("2026-10-21T08:00:00").day());
        assertEquals(LocalDate.of(2026, 10, 21), at("2026-10-22T00:00:00").day());
        assertEquals(LocalDate.of(2026, 10, 22), at("2026-10-22T08:00:00").day());
    }
    @Test void eveningUnlocksExactlyAtEightAndKeepsMorningOpen() {
        assertTrue(at("2026-10-21T08:00:00").unlocked(0));
        assertFalse(at("2026-10-21T19:59:59").unlocked(3));
        assertTrue(at("2026-10-21T20:00:00").unlocked(3));
        assertTrue(at("2026-10-21T20:00:00").unlocked(0));
        assertEquals(Instant.parse("2026-10-21T23:00:00Z"), at("2026-10-21T20:00:00").resetAt());
    }
}
