package me.jianwen.mediask.schedule.domain.entity;

import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppointmentSlot 实体单元测试
 *
 * @author jianwen
 */
class AppointmentSlotTest {

    private static final ScheduleId SCHEDULE_ID = ScheduleId.of(1000L);
    private static final TimeSlot TIME_SLOT = TimeSlot.of(LocalTime.of(9, 0), 15);

    @Test
    @DisplayName("创建可用时段 - 成功")
    void createAvailable_Success() {
        // when
        AppointmentSlot slot = AppointmentSlot.createAvailable(SCHEDULE_ID, TIME_SLOT);

        // then
        assertNotNull(slot);
        assertEquals(SCHEDULE_ID, slot.getScheduleId());
        assertEquals(TIME_SLOT, slot.getTimeSlot());
        assertFalse(slot.isOccupied());
        assertNull(slot.getAppointmentId());
        assertNotNull(slot.getCreatedAt());
        assertNotNull(slot.getUpdatedAt());
    }

    @Test
    @DisplayName("占用时段 - 成功")
    void occupy_Success() {
        // given
        AppointmentSlot slot = AppointmentSlot.createAvailable(SCHEDULE_ID, TIME_SLOT);
        Long appointmentId = 12345L;

        // when
        slot.occupy(appointmentId);

        // then
        assertTrue(slot.isOccupied());
        assertEquals(appointmentId, slot.getAppointmentId());
    }

    @Test
    @DisplayName("占用时段 - 已被占用则抛出异常")
    void occupy_AlreadyOccupied_ThrowsException() {
        // given
        AppointmentSlot slot = AppointmentSlot.createAvailable(SCHEDULE_ID, TIME_SLOT);
        slot.occupy(12345L);

        // when & then
        assertThrows(IllegalStateException.class, () -> slot.occupy(67890L));
    }

    @Test
    @DisplayName("释放时段 - 成功")
    void release_Success() {
        // given
        AppointmentSlot slot = AppointmentSlot.createAvailable(SCHEDULE_ID, TIME_SLOT);
        slot.occupy(12345L);

        // when
        slot.release();

        // then
        assertFalse(slot.isOccupied());
        assertNull(slot.getAppointmentId());
    }

    @Test
    @DisplayName("释放时段 - 幂等性")
    void release_Idempotent() {
        // given
        AppointmentSlot slot = AppointmentSlot.createAvailable(SCHEDULE_ID, TIME_SLOT);

        // when - 多次释放
        slot.release();
        slot.release();
        slot.release();

        // then - 不抛出异常
        assertFalse(slot.isOccupied());
    }

    @Test
    @DisplayName("检查时段是否可用 - 空闲")
    void isAvailable_WhenFree_ReturnsTrue() {
        // given
        AppointmentSlot slot = AppointmentSlot.createAvailable(SCHEDULE_ID, TIME_SLOT);

        // then
        assertTrue(slot.isAvailable());
    }

    @Test
    @DisplayName("检查时段是否可用 - 已占用")
    void isAvailable_WhenOccupied_ReturnsFalse() {
        // given
        AppointmentSlot slot = AppointmentSlot.createAvailable(SCHEDULE_ID, TIME_SLOT);
        slot.occupy(12345L);

        // then
        assertFalse(slot.isAvailable());
    }
}
