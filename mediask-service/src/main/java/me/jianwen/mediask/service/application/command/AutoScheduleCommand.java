package me.jianwen.mediask.service.application.command;

import lombok.Data;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自动排班命令
 */
@Data
public class AutoScheduleCommand {

    private Long doctorId;

    private LocalDate startDate;

    private LocalDate endDate;

    private String strategyName;

    private Set<DayOfWeek> workDays;

    private Set<Integer> timePeriodCodes;

    private Integer slotsPerPeriod;

    private Integer slotDurationMinutes;

    private Boolean excludeHolidays = false;

    public Set<TimePeriod> getTimePeriods() {
        return timePeriodCodes.stream()
                .map(TimePeriod::fromCode)
                .collect(Collectors.toSet());
    }
}
