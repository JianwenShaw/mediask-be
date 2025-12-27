package me.jianwen.mediask.schedule.domain.entity;

import lombok.Data;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimeSlot;

import java.time.LocalDateTime;

/**
 * 号源时段实体
 * 管理具体的时间片和占用状态
 *
 * @author jianwen
 */
@Data
public class AppointmentSlot {

    /**
     * 时段ID
     */
    private Long id;

    /**
     * 所属排班ID
     */
    private ScheduleId scheduleId;

    /**
     * 时间片
     */
    private TimeSlot timeSlot;

    /**
     * 是否已占用
     */
    private boolean occupied;

    /**
     * 预约ID（如果已占用）
     */
    private Long appointmentId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ============ 工厂方法 ============

    /**
     * 创建空闲时段
     */
    public static AppointmentSlot createAvailable(ScheduleId scheduleId, TimeSlot timeSlot) {
        AppointmentSlot slot = new AppointmentSlot();
        slot.setScheduleId(scheduleId);
        slot.setTimeSlot(timeSlot);
        slot.setOccupied(false);
        slot.setCreatedAt(LocalDateTime.now());
        slot.setUpdatedAt(LocalDateTime.now());
        return slot;
    }

    // ============ 业务行为 ============

    /**
     * 占用时段
     */
    public void occupy(Long appointmentId) {
        if (this.occupied) {
            throw new IllegalStateException("时段已被占用");
        }
        this.occupied = true;
        this.appointmentId = appointmentId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 释放时段
     */
    public void release() {
        if (!this.occupied) {
            return; // 幂等性保证
        }
        this.occupied = false;
        this.appointmentId = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查时段是否可用
     */
    public boolean isAvailable() {
        return !occupied;
    }
}
