package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 排班仓储接口（领域层定义）
 *
 * @author jianwen
 */
public interface DoctorScheduleRepository {

    /**
     * 保存排班
     */
    void save(DoctorSchedule schedule);

    /**
     * 批量保存排班
     */
    void saveAll(List<DoctorSchedule> schedules);

    /**
     * 根据ID查询
     */
    Optional<DoctorSchedule> findById(ScheduleId scheduleId);

    /**
     * 查询医生在指定日期和时段的排班
     */
    Optional<DoctorSchedule> findByDoctorAndDateAndPeriod(
            DoctorId doctorId,
            LocalDate scheduleDate,
            TimePeriod timePeriod);

    /**
     * 查询医生在指定日期的所有排班
     */
    List<DoctorSchedule> findByDoctorAndDate(DoctorId doctorId, LocalDate scheduleDate);

    /**
     * 查询医生在日期范围内的所有排班
     */
    List<DoctorSchedule> findByDoctorAndDateRange(
            DoctorId doctorId,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * 查询指定日期和时段的所有开放排班
     */
    List<DoctorSchedule> findOpenSchedulesByDateAndPeriod(
            LocalDate scheduleDate,
            TimePeriod timePeriod);

    /**
     * 检查排班是否存在
     */
    boolean exists(DoctorId doctorId, LocalDate scheduleDate, TimePeriod timePeriod);

    /**
     * 查询已过期的排班
     */
    List<DoctorSchedule> findExpiredSchedules(LocalDate beforeDate);

    /**
     * 删除排班
     */
    void remove(ScheduleId scheduleId);
}
