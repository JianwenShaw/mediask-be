package me.jianwen.mediask.schedule.domain.entity;

import lombok.Data;
import me.jianwen.mediask.schedule.domain.event.ScheduleCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleSlotDecreasedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleSlotIncreasedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleStatusChangedEvent;
import me.jianwen.mediask.schedule.domain.valueobject.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 医生排班聚合根
 * 
 * 职责：
 * 1. 管理排班的基本信息（医生、日期、时段）
 * 2. 控制号源的扣减和释放
 * 3. 管理排班状态的流转
 * 4. 发布领域事件
 *
 * @author jianwen
 */
@Data
public class DoctorSchedule {

    /**
     * 排班ID（唯一标识）
     */
    private ScheduleId id;

    /**
     * 医生ID
     */
    private DoctorId doctorId;

    /**
     * 排班日期
     */
    private LocalDate scheduleDate;

    /**
     * 时段（上午/下午/晚上）
     */
    private TimePeriod timePeriod;

    /**
     * 号源容量
     */
    private SlotCapacity capacity;

    /**
     * 排班状态
     */
    private ScheduleStatus status;

    /**
     * 每个号源的就诊时长（分钟）
     */
    private int slotDurationMinutes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 领域事件集合
     */
    private transient List<Object> domainEvents = new ArrayList<>();

    // ============ 工厂方法 ============

    /**
     * 创建排班
     */
    public static DoctorSchedule create(
            DoctorId doctorId,
            LocalDate scheduleDate,
            TimePeriod timePeriod,
            int totalSlots,
            int slotDurationMinutes) {

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorId(doctorId);
        schedule.setScheduleDate(scheduleDate);
        schedule.setTimePeriod(timePeriod);
        schedule.setCapacity(SlotCapacity.initial(totalSlots));
        schedule.setStatus(ScheduleStatus.OPEN);
        schedule.setSlotDurationMinutes(slotDurationMinutes);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());

        // 发布排班创建事件
        schedule.addDomainEvent(new ScheduleCreatedEvent(
                schedule.getId(),
                schedule.getDoctorId(),
                schedule.getScheduleDate(),
                schedule.getTimePeriod()));

        return schedule;
    }

    // ============ 业务行为 ============

    /**
     * 扣减号源（预约时调用）
     */
    public void decreaseSlot() {
        // 业务规则校验
        if (!status.canAppointment()) {
            throw new IllegalStateException("排班状态不允许预约: " + status.getDescription());
        }

        if (!capacity.hasAvailable()) {
            throw new IllegalStateException("号源已满，无法预约");
        }

        // 扣减号源
        this.capacity = capacity.decrease();
        this.updatedAt = LocalDateTime.now();

        // 如果号源已满，自动变更状态
        if (capacity.isFull()) {
            this.status = ScheduleStatus.FULL;
            addDomainEvent(new ScheduleStatusChangedEvent(
                    this.id,
                    ScheduleStatus.OPEN,
                    ScheduleStatus.FULL));
        }

        // 发布号源扣减事件
        addDomainEvent(new ScheduleSlotDecreasedEvent(
                this.id,
                this.capacity.getAvailableSlots()));
    }

    /**
     * 增加号源（取消预约时调用）
     */
    public void increaseSlot() {
        // 业务规则校验
        if (!status.canCancel()) {
            throw new IllegalStateException("排班状态不允许取消: " + status.getDescription());
        }

        // 增加号源
        boolean wasFull = capacity.isFull();
        this.capacity = capacity.increase();
        this.updatedAt = LocalDateTime.now();

        // 如果之前是约满状态，现在有号源了，恢复为开放
        if (wasFull && status == ScheduleStatus.FULL) {
            this.status = ScheduleStatus.OPEN;
            addDomainEvent(new ScheduleStatusChangedEvent(
                    this.id,
                    ScheduleStatus.FULL,
                    ScheduleStatus.OPEN));
        }

        // 发布号源增加事件
        addDomainEvent(new ScheduleSlotIncreasedEvent(
                this.id,
                this.capacity.getAvailableSlots()));
    }

    /**
     * 停诊（医生请假、调休等）
     */
    public void close(String reason) {
        if (this.status == ScheduleStatus.CLOSED) {
            return; // 幂等性保证
        }

        ScheduleStatus oldStatus = this.status;
        this.status = ScheduleStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new ScheduleStatusChangedEvent(
                this.id,
                oldStatus,
                ScheduleStatus.CLOSED));
    }

    /**
     * 开诊（取消停诊）
     */
    public void open() {
        if (this.status == ScheduleStatus.OPEN) {
            return; // 幂等性保证
        }

        if (this.status == ScheduleStatus.EXPIRED) {
            throw new IllegalStateException("已过期的排班无法重新开诊");
        }

        ScheduleStatus oldStatus = this.status;

        // 如果号源已满，状态为FULL，否则为OPEN
        this.status = capacity.isFull() ? ScheduleStatus.FULL : ScheduleStatus.OPEN;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new ScheduleStatusChangedEvent(
                this.id,
                oldStatus,
                this.status));
    }

    /**
     * 标记为已过期
     */
    public void markAsExpired() {
        if (this.status == ScheduleStatus.EXPIRED) {
            return; // 幂等性保证
        }

        ScheduleStatus oldStatus = this.status;
        this.status = ScheduleStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new ScheduleStatusChangedEvent(
                this.id,
                oldStatus,
                ScheduleStatus.EXPIRED));
    }

    /**
     * 调整总号源数（管理员手动调整）
     */
    public void adjustTotalSlots(int newTotalSlots) {
        if (newTotalSlots < capacity.getUsedSlots()) {
            throw new IllegalArgumentException(
                    "新的总号源数不能小于已使用的号源数: " + capacity.getUsedSlots());
        }

        int newAvailableSlots = newTotalSlots - capacity.getUsedSlots();
        this.capacity = new SlotCapacity(newTotalSlots, newAvailableSlots);
        this.updatedAt = LocalDateTime.now();

        // 如果之前约满，现在有号源了，恢复开放状态
        if (this.status == ScheduleStatus.FULL && capacity.hasAvailable()) {
            this.status = ScheduleStatus.OPEN;
            addDomainEvent(new ScheduleStatusChangedEvent(
                    this.id,
                    ScheduleStatus.FULL,
                    ScheduleStatus.OPEN));
        }
    }

    /**
     * 检查是否在可预约时间范围内
     * 业务规则：只能预约未来7天内的号
     */
    public boolean isInAppointmentPeriod() {
        LocalDate now = LocalDate.now();
        LocalDate maxDate = now.plusDays(7);
        return !scheduleDate.isBefore(now) && !scheduleDate.isAfter(maxDate);
    }

    /**
     * 检查排班日期是否已过期
     */
    public boolean isExpired() {
        return scheduleDate.isBefore(LocalDate.now());
    }

    /**
     * 生成时间片列表
     * 根据时段和每个号源的时长，生成具体的时间片
     */
    public List<TimeSlot> generateTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        LocalTime currentTime = timePeriod.getStartTime();
        LocalTime endTime = timePeriod.getEndTime();

        while (currentTime.plusMinutes(slotDurationMinutes).isBefore(endTime)
                || currentTime.plusMinutes(slotDurationMinutes).equals(endTime)) {
            timeSlots.add(TimeSlot.of(currentTime, slotDurationMinutes));
            currentTime = currentTime.plusMinutes(slotDurationMinutes);
        }

        return timeSlots;
    }

    // ============ 领域事件管理 ============

    /**
     * 添加领域事件
     */
    private void addDomainEvent(Object event) {
        this.domainEvents.add(event);
    }

    /**
     * 获取领域事件
     */
    public List<Object> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    /**
     * 清除领域事件
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
