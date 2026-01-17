package me.jianwen.mediask.schedule.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppointmentStatus 值对象单元测试
 *
 * @author jianwen
 */
class AppointmentStatusTest {

    @Test
    @DisplayName("从有效状态码获取枚举")
    void fromCode_ValidCode_ReturnsEnum() {
        assertEquals(AppointmentStatus.UNPAID, AppointmentStatus.fromCode(1));
        assertEquals(AppointmentStatus.CONFIRMED, AppointmentStatus.fromCode(2));
        assertEquals(AppointmentStatus.VISITED, AppointmentStatus.fromCode(3));
        assertEquals(AppointmentStatus.CANCELLED, AppointmentStatus.fromCode(4));
        assertEquals(AppointmentStatus.ABSENT, AppointmentStatus.fromCode(5));
    }

    @Test
    @DisplayName("从无效状态码获取枚举 - 抛出异常")
    void fromCode_InvalidCode_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> AppointmentStatus.fromCode(0));
        assertThrows(IllegalArgumentException.class, () -> AppointmentStatus.fromCode(6));
        assertThrows(IllegalArgumentException.class, () -> AppointmentStatus.fromCode(999));
    }

    @ParameterizedTest
    @DisplayName("canCancel - 状态是否可取消")
    @CsvSource({
            "1, true",   // UNPAID
            "2, true",   // CONFIRMED
            "3, false",  // VISITED
            "4, false",  // CANCELLED
            "5, false"   // ABSENT
    })
    void canCancel_ReturnsCorrectValue(int statusCode, boolean expected) {
        AppointmentStatus status = AppointmentStatus.fromCode(statusCode);
        assertEquals(expected, status.canCancel());
    }

    @ParameterizedTest
    @DisplayName("canPay - 状态是否可支付")
    @CsvSource({
            "1, true",   // UNPAID
            "2, false",  // CONFIRMED
            "3, false",  // VISITED
            "4, false",  // CANCELLED
            "5, false"   // ABSENT
    })
    void canPay_ReturnsCorrectValue(int statusCode, boolean expected) {
        AppointmentStatus status = AppointmentStatus.fromCode(statusCode);
        assertEquals(expected, status.canPay());
    }

    @ParameterizedTest
    @DisplayName("canMarkVisited - 状态是否可标记就诊")
    @CsvSource({
            "1, false",  // UNPAID
            "2, true",   // CONFIRMED
            "3, false",  // VISITED
            "4, false",  // CANCELLED
            "5, false"   // ABSENT
    })
    void canMarkVisited_ReturnsCorrectValue(int statusCode, boolean expected) {
        AppointmentStatus status = AppointmentStatus.fromCode(statusCode);
        assertEquals(expected, status.canMarkVisited());
    }

    @ParameterizedTest
    @DisplayName("isCancelled - 是否已取消")
    @CsvSource({
            "1, false",  // UNPAID
            "2, false",  // CONFIRMED
            "3, false",  // VISITED
            "4, true",   // CANCELLED
            "5, false"   // ABSENT
    })
    void isCancelled_ReturnsCorrectValue(int statusCode, boolean expected) {
        AppointmentStatus status = AppointmentStatus.fromCode(statusCode);
        assertEquals(expected, status.isCancelled());
    }

    @ParameterizedTest
    @DisplayName("isTerminal - 是否为终态")
    @CsvSource({
            "1, false",  // UNPAID
            "2, false",  // CONFIRMED
            "3, true",   // VISITED
            "4, true",   // CANCELLED
            "5, true"    // ABSENT
    })
    void isTerminal_ReturnsCorrectValue(int statusCode, boolean expected) {
        AppointmentStatus status = AppointmentStatus.fromCode(statusCode);
        assertEquals(expected, status.isTerminal());
    }

    @Test
    @DisplayName("状态码与描述对应")
    void statusCodeAndDescription_Correspond() {
        assertAll(
                () -> assertEquals(1, AppointmentStatus.UNPAID.code()),
                () -> assertEquals("待支付", AppointmentStatus.UNPAID.description()),
                () -> assertEquals(2, AppointmentStatus.CONFIRMED.code()),
                () -> assertEquals("已预约", AppointmentStatus.CONFIRMED.description()),
                () -> assertEquals(3, AppointmentStatus.VISITED.code()),
                () -> assertEquals("已就诊", AppointmentStatus.VISITED.description()),
                () -> assertEquals(4, AppointmentStatus.CANCELLED.code()),
                () -> assertEquals("已取消", AppointmentStatus.CANCELLED.description()),
                () -> assertEquals(5, AppointmentStatus.ABSENT.code()),
                () -> assertEquals("爽约", AppointmentStatus.ABSENT.description())
        );
    }
}
