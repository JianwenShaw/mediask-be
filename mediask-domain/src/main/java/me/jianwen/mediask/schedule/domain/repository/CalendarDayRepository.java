package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.optimization.model.CalendarDayRule;

import java.time.LocalDate;
import java.util.List;

public interface CalendarDayRepository {

    List<CalendarDayRule> listByDateRange(LocalDate startDate, LocalDate endDate, String regionCode);
}
