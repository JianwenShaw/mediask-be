package me.jianwen.mediask.schedule.domain.entity;

import me.jianwen.mediask.schedule.domain.valueobject.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医生排班 领域实体（简化版，用于当前编译）
 */
public class DoctorSchedule {

    private ScheduleId id;
    private DoctorId doctorId;
    private LocalDate scheduleDate;
    private TimePeriod timePeriod;
    private SlotCapacity capacity;
    private ScheduleStatus status;
    private Integer slotDurationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ScheduleId getId() {
        return id;
    }

    public void setId(ScheduleId id) {
        this.id = id;
    }

    public DoctorId getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(DoctorId doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(TimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

    public SlotCapacity getCapacity() {
        return capacity;
    }

    public void setCapacity(SlotCapacity capacity) {
        this.capacity = capacity;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public Integer getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(Integer slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

