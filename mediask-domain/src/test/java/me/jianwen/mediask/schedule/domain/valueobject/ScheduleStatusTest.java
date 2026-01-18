package me.jianwen.mediask.schedule.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScheduleStatus 枚举单元测试
 *
 * @author jianwen
 */
class ScheduleStatusTest {

    @Test
    @DisplayName("状态码映射正确")
    void fromCode_ReturnsCorrectStatus() {
        assertEquals(ScheduleStatus.CLOSED, ScheduleStatus.fromCode(0));
        assertEquals(ScheduleStatus.OPEN, ScheduleStatus.fromCode(1));
        assertEquals(ScheduleStatus.FULL, ScheduleStatus.fromCode(2));
        assertEquals(ScheduleStatus.EXPIRED, ScheduleStatus.fromCode(3));
    }

    @ParameterizedTest
    @DisplayName("状态码无效时抛出异常")
    @CsvSource({"-1", "4", "100"})
    void fromCode_InvalidCode_ThrowsException(int code) {
        assertThrows(IllegalArgumentException.class, () -> ScheduleStatus.fromCode(code));
    }

    @Test
    @DisplayName("OPEN状态可以预约")
    void canAppointment_ReturnsTrueForOpen() {
        assertTrue(ScheduleStatus.OPEN.canAppointment());
    }

    @Test
    @DisplayName("CLOSED状态不可以预约")
    void canAppointment_ReturnsFalseForClosed() {
        assertFalse(ScheduleStatus.CLOSED.canAppointment());
    }

    @Test
    @DisplayName("FULL状态不可以预约")
    void canAppointment_ReturnsFalseForFull() {
        assertFalse(ScheduleStatus.FULL.canAppointment());
    }

    @Test
    @DisplayName("EXPIRED状态不可以预约")
    void canAppointment_ReturnsFalseForExpired() {
        assertFalse(ScheduleStatus.EXPIRED.canAppointment());
    }

    @Test
    @DisplayName("OPEN状态可以取消")
    void canCancel_ReturnsTrueForOpen() {
        assertTrue(ScheduleStatus.OPEN.canCancel());
    }

    @Test
    @DisplayName("FULL状态可以取消")
    void canCancel_ReturnsTrueForFull() {
        assertTrue(ScheduleStatus.FULL.canCancel());
    }

    @Test
    @DisplayName("CLOSED状态不可以取消")
    void canCancel_ReturnsFalseForClosed() {
        assertFalse(ScheduleStatus.CLOSED.canCancel());
    }

    @Test
    @DisplayName("EXPIRED状态不可以取消")
    void canCancel_ReturnsFalseForExpired() {
        assertFalse(ScheduleStatus.EXPIRED.canCancel());
    }

    @Test
    @DisplayName("状态描述正确")
    void getDescription_ReturnsCorrectDescription() {
        assertEquals("停诊", ScheduleStatus.CLOSED.getDescription());
        assertEquals("开放", ScheduleStatus.OPEN.getDescription());
        assertEquals("约满", ScheduleStatus.FULL.getDescription());
        assertEquals("已过期", ScheduleStatus.EXPIRED.getDescription());
    }

    @Test
    @DisplayName("状态码正确")
    void getCode_ReturnsCorrectCode() {
        assertEquals(0, ScheduleStatus.CLOSED.getCode());
        assertEquals(1, ScheduleStatus.OPEN.getCode());
        assertEquals(2, ScheduleStatus.FULL.getCode());
        assertEquals(3, ScheduleStatus.EXPIRED.getCode());
    }
}
