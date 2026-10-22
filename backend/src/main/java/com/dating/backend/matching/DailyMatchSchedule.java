package com.dating.backend.matching;

import org.springframework.stereotype.Component;
import java.time.*;

@Component
public class DailyMatchSchedule {
    static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final Clock clock;
    public DailyMatchSchedule() { this(Clock.systemUTC()); }
    DailyMatchSchedule(Clock clock) { this.clock = clock; }
    public Window current() { return at(clock.instant()); }
    static Window at(Instant now) {
        var local = now.atZone(ZONE);
        var day = local.toLocalTime().isBefore(LocalTime.of(8, 0))
                ? local.toLocalDate().minusDays(1) : local.toLocalDate();
        return new Window(now, day, day.atTime(8, 0).atZone(ZONE).toInstant(),
                day.atTime(20, 0).atZone(ZONE).toInstant(),
                day.plusDays(1).atTime(8, 0).atZone(ZONE).toInstant());
    }
    public record Window(Instant now, LocalDate day, Instant morning, Instant evening, Instant resetAt) {
        public Instant unlockAt(int slot) { return slot < 3 || slot >= 6 ? morning : evening; }
        public boolean unlocked(int slot) { return !now.isBefore(unlockAt(slot)); }
    }
}
