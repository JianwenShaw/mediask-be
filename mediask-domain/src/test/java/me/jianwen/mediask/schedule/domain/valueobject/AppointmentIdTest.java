package me.jianwen.mediask.schedule.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppointmentId 值对象单元测试
 *
 * @author jianwen
 */
class AppointmentIdTest {

    @Test
    @DisplayName("创建 AppointmentId - 成功")
    void of_ValidValue_Success() {
        AppointmentId appointmentId = AppointmentId.of(1000L);
        assertEquals(1000L, appointmentId.value());
    }

    @Test
    @DisplayName("创建 AppointmentId - null 抛出异常")
    void of_Null_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> AppointmentId.of(null));
    }

    @Test
    @DisplayName("创建 AppointmentId - 0 抛出异常")
    void of_Zero_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> AppointmentId.of(0L));
    }

    @Test
    @DisplayName("创建 AppointmentId - 负数抛出异常")
    void of_Negative_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> AppointmentId.of(-1L));
    }

    @Test
    @DisplayName("AppointmentId 相等性")
    void equals_SameValue_Equal() {
        AppointmentId id1 = AppointmentId.of(1000L);
        AppointmentId id2 = AppointmentId.of(1000L);
        assertEquals(id1, id2);
    }

    @Test
    @DisplayName("AppointmentId 不相等")
    void equals_DifferentValue_NotEqual() {
        AppointmentId id1 = AppointmentId.of(1000L);
        AppointmentId id2 = AppointmentId.of(2000L);
        assertNotEquals(id1, id2);
    }
}
