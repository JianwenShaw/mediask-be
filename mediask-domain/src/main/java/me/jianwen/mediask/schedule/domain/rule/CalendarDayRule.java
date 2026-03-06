package me.jianwen.mediask.schedule.domain.rule;

import java.time.LocalDate;

public record CalendarDayRule(
        LocalDate date,
        boolean holiday,
        boolean makeupWorkday,
        String holidayName
) {
}
