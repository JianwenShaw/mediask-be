package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 排班仓储接口（简化版，用于当前编译）
 */
public interface DoctorScheduleRepository {

    void save(DoctorSchedule schedule);

    void saveAll(List<DoctorSchedule> schedules);

    Optional<DoctorSchedule> findById(ScheduleId scheduleId);

    Optional<DoctorSchedule> findByDoctorAndDateAndPeriod(DoctorId doctorId, LocalDate scheduleDate, TimePeriod timePeriod);

    List<DoctorSchedule> findByDoctorAndDate(DoctorId doctorId, LocalDate scheduleDate);

    List<DoctorSchedule> findByDoctorAndDateRange(DoctorId doctorId, LocalDate startDate, LocalDate endDate);

    List<DoctorSchedule> findOpenSchedulesByDateAndPeriod(LocalDate scheduleDate, TimePeriod timePeriod);

    boolean exists(DoctorId doctorId, LocalDate scheduleDate, TimePeriod timePeriod);

    List<DoctorSchedule> findExpiredSchedules(LocalDate beforeDate);

    void remove(ScheduleId scheduleId);
}

