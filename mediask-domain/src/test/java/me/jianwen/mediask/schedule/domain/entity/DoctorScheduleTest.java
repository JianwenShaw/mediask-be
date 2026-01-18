package me.jianwen.mediask.schedule.domain.entity;

import me.jianwen.mediask.schedule.domain.event.ScheduleCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleSlotDecreasedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleSlotIncreasedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleStatusChangedEvent;
import me.jianwen.mediask.schedule.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DoctorSchedule 聚合根单元测试
 *
 * @author jianwen
 */
class DoctorScheduleTest {

    private static final DoctorId DOCTOR_ID = DoctorId.of(100L);
    private static final LocalDate SCHEDULE_DATE = LocalDate.now().plusDays(1);
    private static final TimePeriod TIME_PERIOD = TimePeriod.MORNING;
    private static final int TOTAL_SLOTS = 20;
    private static final int SLOT_DURATION = 15;

    @Test
    @DisplayName("创建排班 - 成功")
    void createSchedule_Success() {
        // when
        DoctorSchedule schedule = DoctorSchedule.create(
                DOCTOR_ID, SCHEDULE_DATE, TIME_PERIOD, TOTAL_SLOTS, SLOT_DURATION);

        // then
        assertNotNull(schedule);
        assertEquals(DOCTOR_ID, schedule.getDoctorId());
        assertEquals(SCHEDULE_DATE, schedule.getScheduleDate());
        assertEquals(TIME_PERIOD, schedule.getTimePeriod());
        assertEquals(TOTAL_SLOTS, schedule.getCapacity().getTotalSlots());
        assertEquals(TOTAL_SLOTS, schedule.getCapacity().getAvailableSlots());
        assertEquals(ScheduleStatus.OPEN, schedule.getStatus());
        assertEquals(SLOT_DURATION, schedule.getSlotDurationMinutes());
        assertNotNull(schedule.getCreatedAt());
    }

    @Test
    @DisplayName("创建排班 - 发布领域事件")
    void createSchedule_PublishesDomainEvent() {
        // when
        DoctorSchedule schedule = DoctorSchedule.create(
                DOCTOR_ID, SCHEDULE_DATE, TIME_PERIOD, TOTAL_SLOTS, SLOT_DURATION);

        // then
        assertEquals(1, schedule.getDomainEvents().size());
        assertInstanceOf(ScheduleCreatedEvent.class, schedule.getDomainEvents().get(0));
    }

    @Test
    @DisplayName("扣减号源 - 成功")
    void decreaseSlot_Success() {
        // given
        DoctorSchedule schedule = createTestSchedule();
        int initialAvailable = schedule.getCapacity().getAvailableSlots();

        // when
        schedule.decreaseSlot();

        // then
        assertEquals(initialAvailable - 1, schedule.getCapacity().getAvailableSlots());
        assertEquals(1, schedule.getCapacity().getUsedSlots());
    }

    @Test
    @DisplayName("扣减号源 - 号源已满自动变更状态")
    void decreaseSlot_FullToOpen_ChangesStatus() {
        // given
        DoctorSchedule schedule = DoctorSchedule.create(
                DOCTOR_ID, SCHEDULE_DATE, TIME_PERIOD, 1, SLOT_DURATION);
        schedule.clearDomainEvents();

        // when
        schedule.decreaseSlot();

        // then
        assertEquals(ScheduleStatus.FULL, schedule.getStatus());
    }

    @Test
    @DisplayName("扣减号源 - 停诊状态不允许预约")
    void decreaseSlot_Closed_ThrowsException() {
        // given
        DoctorSchedule schedule = createTestSchedule();
        schedule.close("医生请假");
        schedule.clearDomainEvents();

        // when & then
        assertThrows(IllegalStateException.class, schedule::decreaseSlot);
    }

    @Test
    @DisplayName("增加号源 - 成功")
    void increaseSlot_Success() {
        // given
        DoctorSchedule schedule = createTestSchedule();
        schedule.decreaseSlot();
        schedule.clearDomainEvents();
        int initialAvailable = schedule.getCapacity().getAvailableSlots();

        // when
        schedule.increaseSlot();

        // then
        assertEquals(initialAvailable + 1, schedule.getCapacity().getAvailableSlots());
    }

    @Test
    @DisplayName("增加号源 - 约满状态恢复开放")
    void increaseSlot_FullToOpen_ChangesStatus() {
        // given
        DoctorSchedule schedule = DoctorSchedule.create(
                DOCTOR_ID, SCHEDULE_DATE, TIME_PERIOD, 1, SLOT_DURATION);
        schedule.decreaseSlot();
        schedule.clearDomainEvents();

        // when
        schedule.increaseSlot();

        // then
        assertEquals(ScheduleStatus.OPEN, schedule.getStatus());
    }

    @Test
    @DisplayName("停诊 - 成功")
    void close_Success() {
        // given
        DoctorSchedule schedule = createTestSchedule();

        // when
        schedule.close("医生请假");

        // then
        assertEquals(ScheduleStatus.CLOSED, schedule.getStatus());
    }

    @Test
    @DisplayName("停诊 - 幂等性")
    void close_Idempotent() {
        // given
        DoctorSchedule schedule = createTestSchedule();
        int initialEvents = schedule.getDomainEvents().size();

        // when - 第一次调用
        schedule.close("第一次停诊");
        int eventsAfterFirst = schedule.getDomainEvents().size() - initialEvents;

        // then - 第一次调用应该产生1个事件
        assertEquals(ScheduleStatus.CLOSED, schedule.getStatus());
        assertEquals(1, eventsAfterFirst);

        // when - 第二次调用
        schedule.close("第二次停诊");
        int eventsAfterSecond = schedule.getDomainEvents().size() - initialEvents;

        // then - 第二次调用不应产生新事件（幂等性）
        assertEquals(1, eventsAfterSecond);
    }

    @Test
    @DisplayName("开诊 - 成功")
    void open_Success() {
        // given
        DoctorSchedule schedule = createTestSchedule();
        schedule.close("医生请假");
        schedule.clearDomainEvents();

        // when
        schedule.open();

        // then
        assertEquals(ScheduleStatus.OPEN, schedule.getStatus());
    }

    @Test
    @DisplayName("开诊 - 已过期不允许开诊")
    void open_Expired_ThrowsException() {
        // given
        DoctorSchedule schedule = createTestSchedule();
        schedule.markAsExpired();
        schedule.clearDomainEvents();

        // when & then
        assertThrows(IllegalStateException.class, schedule::open);
    }

    @Test
    @DisplayName("标记已过期 - 成功")
    void markAsExpired_Success() {
        // given
        DoctorSchedule schedule = createTestSchedule();

        // when
        schedule.markAsExpired();

        // then
        assertEquals(ScheduleStatus.EXPIRED, schedule.getStatus());
    }

    @Test
    @DisplayName("调整号源数 - 成功")
    void adjustTotalSlots_Success() {
        // given
        DoctorSchedule schedule = createTestSchedule();
        int newTotalSlots = 30;

        // when
        schedule.adjustTotalSlots(newTotalSlots);

        // then
        assertEquals(newTotalSlots, schedule.getCapacity().getTotalSlots());
        assertEquals(newTotalSlots, schedule.getCapacity().getAvailableSlots());
    }

    @Test
    @DisplayName("调整号源数 - 新号源数不能小于已使用")
    void adjustTotalSlots_LessThanUsed_ThrowsException() {
        // given
        DoctorSchedule schedule = DoctorSchedule.create(
                DOCTOR_ID, SCHEDULE_DATE, TIME_PERIOD, 10, SLOT_DURATION);
        schedule.decreaseSlot();
        schedule.clearDomainEvents();

        // when & then - 已使用1个号源，尝试调整为0应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> schedule.adjustTotalSlots(0));
    }

    @Test
    @DisplayName("生成时间片 - 成功")
    void generateTimeSlots_Success() {
        // given
        DoctorSchedule schedule = DoctorSchedule.create(
                DOCTOR_ID, SCHEDULE_DATE, TimePeriod.MORNING, 10, 30);

        // when
        List<TimeSlot> timeSlots = schedule.generateTimeSlots();

        // then
        assertFalse(timeSlots.isEmpty());
        LocalTime expectedStart = TimePeriod.MORNING.getStartTime();
        assertEquals(expectedStart, timeSlots.get(0).getStartTime());
    }

    @Test
    @DisplayName("检查是否在预约时间范围内")
    void isInAppointmentPeriod_ReturnsCorrectValue() {
        // given
        DoctorSchedule futureSchedule = DoctorSchedule.create(
                DOCTOR_ID, LocalDate.now().plusDays(3), TIME_PERIOD, TOTAL_SLOTS, SLOT_DURATION);
        DoctorSchedule pastSchedule = DoctorSchedule.create(
                DOCTOR_ID, LocalDate.now().minusDays(1), TIME_PERIOD, TOTAL_SLOTS, SLOT_DURATION);

        // then
        assertTrue(futureSchedule.isInAppointmentPeriod());
        assertFalse(pastSchedule.isInAppointmentPeriod());
    }

    @Test
    @DisplayName("检查排班日期是否已过期")
    void isExpired_ReturnsCorrectValue() {
        // given
        DoctorSchedule futureSchedule = DoctorSchedule.create(
                DOCTOR_ID, LocalDate.now().plusDays(3), TIME_PERIOD, TOTAL_SLOTS, SLOT_DURATION);
        DoctorSchedule pastSchedule = DoctorSchedule.create(
                DOCTOR_ID, LocalDate.now().minusDays(1), TIME_PERIOD, TOTAL_SLOTS, SLOT_DURATION);

        // then
        assertFalse(futureSchedule.isExpired());
        assertTrue(pastSchedule.isExpired());
    }

    @Test
    @DisplayName("号源扣减事件包含正确信息")
    void slotDecreasedEvent_ContainsCorrectInfo() {
        // given
        DoctorSchedule schedule = createTestSchedule();
        schedule.clearDomainEvents();

        // when
        schedule.decreaseSlot();

        // then
        List<Object> events = new ArrayList<>(schedule.getDomainEvents());
        schedule.clearDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(ScheduleSlotDecreasedEvent.class, events.get(0));
    }

    @Test
    @DisplayName("号源增加事件包含正确信息")
    void slotIncreasedEvent_ContainsCorrectInfo() {
        // given
        DoctorSchedule schedule = createTestSchedule();
        schedule.decreaseSlot();
        schedule.clearDomainEvents();

        // when
        schedule.increaseSlot();

        // then
        List<Object> events = new ArrayList<>(schedule.getDomainEvents());
        schedule.clearDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(ScheduleSlotIncreasedEvent.class, events.get(0));
    }

    @Test
    @DisplayName("状态变更事件包含正确信息")
    void statusChangedEvent_ContainsCorrectInfo() {
        // given
        DoctorSchedule schedule = createTestSchedule();
        schedule.clearDomainEvents();

        // when
        schedule.close("测试停诊");

        // then
        List<Object> events = new ArrayList<>(schedule.getDomainEvents());
        schedule.clearDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(ScheduleStatusChangedEvent.class, events.get(0));
        ScheduleStatusChangedEvent event = (ScheduleStatusChangedEvent) events.get(0);
        assertEquals(ScheduleStatus.OPEN, event.getOldStatus());
        assertEquals(ScheduleStatus.CLOSED, event.getNewStatus());
    }

    // ============ 辅助方法 ============

    private DoctorSchedule createTestSchedule() {
        return DoctorSchedule.create(
                DOCTOR_ID, SCHEDULE_DATE, TIME_PERIOD, TOTAL_SLOTS, SLOT_DURATION);
    }
}
