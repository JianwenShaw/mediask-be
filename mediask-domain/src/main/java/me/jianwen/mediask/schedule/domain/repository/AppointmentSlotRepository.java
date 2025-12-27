package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimeSlot;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 号源时段仓储接口
 *
 * @author jianwen
 */
public interface AppointmentSlotRepository {

    /**
     * 保存时段
     */
    void save(AppointmentSlot slot);

    /**
     * 批量保存时段
     */
    void saveAll(List<AppointmentSlot> slots);

    /**
     * 根据ID查询
     */
    Optional<AppointmentSlot> findById(Long id);

    /**
     * 查询排班的所有时段
     */
    List<AppointmentSlot> findBySchedule(ScheduleId scheduleId);

    /**
     * 查询排班的可用时段
     */
    List<AppointmentSlot> findAvailableBySchedule(ScheduleId scheduleId);

    /**
     * 查询排班在指定时间的时段
     */
    Optional<AppointmentSlot> findByScheduleAndTime(ScheduleId scheduleId, LocalTime startTime);

    /**
     * 统计排班的可用时段数
     */
    long countAvailableBySchedule(ScheduleId scheduleId);

    /**
     * 删除排班的所有时段
     */
    void deleteBySchedule(ScheduleId scheduleId);
}
