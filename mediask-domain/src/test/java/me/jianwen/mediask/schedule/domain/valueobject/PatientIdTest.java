package me.jianwen.mediask.schedule.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PatientId 值对象单元测试
 *
 * @author jianwen
 */
class PatientIdTest {

    @Test
    @DisplayName("创建 PatientId - 成功")
    void of_ValidValue_Success() {
        PatientId patientId = PatientId.of(1L);
        assertEquals(1L, patientId.value());
    }

    @Test
    @DisplayName("创建 PatientId - 边界值")
    void of_BoundaryValue_Success() {
        PatientId patientId = PatientId.of(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, patientId.value());
    }

    @Test
    @DisplayName("创建 PatientId - null 抛出异常")
    void of_Null_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> PatientId.of(null));
    }

    @Test
    @DisplayName("创建 PatientId - 0 抛出异常")
    void of_Zero_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> PatientId.of(0L));
    }

    @Test
    @DisplayName("创建 PatientId - 负数抛出异常")
    void of_Negative_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> PatientId.of(-1L));
    }

    @Test
    @DisplayName("PatientId 相等性")
    void equals_SameValue_Equal() {
        PatientId id1 = PatientId.of(1L);
        PatientId id2 = PatientId.of(1L);
        assertEquals(id1, id2);
    }

    @Test
    @DisplayName("PatientId 不相等")
    void equals_DifferentValue_NotEqual() {
        PatientId id1 = PatientId.of(1L);
        PatientId id2 = PatientId.of(2L);
        assertNotEquals(id1, id2);
    }
}
