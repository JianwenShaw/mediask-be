package me.jianwen.mediask.schedule.domain.entity;

import me.jianwen.mediask.schedule.domain.event.AppointmentCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.AppointmentStatusChangedEvent;
import me.jianwen.mediask.schedule.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Appointment 聚合根单元测试
 *
 * @author jianwen
 */
class AppointmentTest {

    private static final PatientId PATIENT_ID = PatientId.of(1L);
    private static final DoctorId DOCTOR_ID = DoctorId.of(100L);
    private static final ScheduleId SCHEDULE_ID = ScheduleId.of(1000L);
    private static final LocalDate APPT_DATE = LocalDate.now().plusDays(1);
    private static final TimePeriod TIME_PERIOD = TimePeriod.MORNING;
    private static final LocalTime APPT_TIME = LocalTime.of(9, 0);
    private static final String APPT_NO = "APPT123456";
    private static final BigDecimal APPT_FEE = BigDecimal.valueOf(50);

    @Test
    @DisplayName("创建预约 - 成功")
    void createAppointment_Success() {
        // given
        String chiefComplaint = "头痛发热";

        // when
        Appointment appointment = Appointment.create(
                PATIENT_ID,
                DOCTOR_ID,
                SCHEDULE_ID,
                APPT_DATE,
                TIME_PERIOD,
                APPT_TIME,
                APPT_FEE,
                chiefComplaint,
                APPT_NO);

        // then
        assertNotNull(appointment);
        assertEquals(PATIENT_ID, appointment.getPatientId());
        assertEquals(DOCTOR_ID, appointment.getDoctorId());
        assertEquals(SCHEDULE_ID, appointment.getScheduleId());
        assertEquals(APPT_DATE, appointment.getApptDate());
        assertEquals(TIME_PERIOD, appointment.getTimePeriod());
        assertEquals(APPT_TIME, appointment.getApptTime());
        assertEquals(APPT_FEE, appointment.getApptFee());
        assertEquals(chiefComplaint, appointment.getChiefComplaint());
        assertEquals(APPT_NO, appointment.getApptNo());
        assertEquals(AppointmentStatus.UNPAID, appointment.getStatus());
        assertNotNull(appointment.getCreatedAt());
    }

    @Test
    @DisplayName("创建预约 - 发布领域事件")
    void createAppointment_PublishesDomainEvent() {
        // given & when
        Appointment appointment = Appointment.create(
                PATIENT_ID, DOCTOR_ID, SCHEDULE_ID, APPT_DATE, TIME_PERIOD,
                APPT_TIME, APPT_FEE, "症状", APPT_NO);

        // then
        assertEquals(1, appointment.getDomainEvents().size());
        assertInstanceOf(AppointmentCreatedEvent.class, appointment.getDomainEvents().get(0));
    }

    @Test
    @DisplayName("支付预约 - 成功")
    void markAsPaid_Success() {
        // given
        Appointment appointment = createTestAppointment();

        // when
        appointment.markAsPaid();

        // then
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        assertNotNull(appointment.getPaidAt());
    }

    @Test
    @DisplayName("支付预约 - 已支付状态不允许再次支付")
    void markAsPaid_AlreadyPaid_ThrowsException() {
        // given
        Appointment appointment = createTestAppointment();
        appointment.markAsPaid();

        // when & then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                appointment::markAsPaid);
        assertTrue(exception.getMessage().contains("支付"));
    }

    @Test
    @DisplayName("取消预约 - 成功")
    void cancel_Success() {
        // given
        Appointment appointment = createTestAppointment();

        // when
        appointment.cancel("因故无法就诊");

        // then
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
    }

    @Test
    @DisplayName("取消预约 - 已就诊状态不允许取消")
    void cancel_AlreadyVisited_ThrowsException() {
        // given
        Appointment appointment = createTestAppointment();
        appointment.markAsPaid();
        appointment.markAsVisited();

        // when & then
        assertThrows(IllegalStateException.class, () -> appointment.cancel("原因"));
    }

    @Test
    @DisplayName("标记已就诊 - 成功")
    void markAsVisited_Success() {
        // given
        Appointment appointment = createTestAppointment();
        appointment.markAsPaid();

        // when
        appointment.markAsVisited();

        // then
        assertEquals(AppointmentStatus.VISITED, appointment.getStatus());
        assertNotNull(appointment.getVisitedAt());
    }

    @Test
    @DisplayName("标记已就诊 - 待支付状态不允许标记")
    void markAsVisited_Unpaid_ThrowsException() {
        // given
        Appointment appointment = createTestAppointment();

        // when & then
        assertThrows(IllegalStateException.class, appointment::markAsVisited);
    }

    @Test
    @DisplayName("标记爽约 - 成功")
    void markAsAbsent_Success() {
        // given
        Appointment appointment = createTestAppointment();
        appointment.markAsPaid();

        // when
        appointment.markAsAbsent();

        // then
        assertEquals(AppointmentStatus.ABSENT, appointment.getStatus());
    }

    @Test
    @DisplayName("标记爽约 - 终态预约不允许标记")
    void markAsAbsent_TerminalStatus_ThrowsException() {
        // given
        Appointment appointment = createTestAppointment();
        appointment.markAsPaid();
        appointment.markAsVisited();

        // when & then
        assertThrows(IllegalStateException.class, appointment::markAsAbsent);
    }

    @Test
    @DisplayName("检查是否可取消")
    void canCancel_ReturnsCorrectValue() {
        // given
        Appointment unpaidAppointment = createTestAppointment();
        Appointment paidAppointment = createTestAppointment();
        paidAppointment.markAsPaid();
        Appointment visitedAppointment = createTestAppointment();
        visitedAppointment.markAsPaid();
        visitedAppointment.markAsVisited();

        // then
        assertTrue(unpaidAppointment.canCancel());
        assertTrue(paidAppointment.canCancel());
        assertFalse(visitedAppointment.canCancel());
    }

    @Test
    @DisplayName("检查是否可支付")
    void canPay_ReturnsCorrectValue() {
        // given
        Appointment unpaidAppointment = createTestAppointment();
        Appointment paidAppointment = createTestAppointment();
        paidAppointment.markAsPaid();

        // then
        assertTrue(unpaidAppointment.canPay());
        assertFalse(paidAppointment.canPay());
    }

    @Test
    @DisplayName("清除领域事件")
    void clearDomainEvents_RemovesAllEvents() {
        // given
        Appointment appointment = createTestAppointment();

        // when
        appointment.clearDomainEvents();

        // then
        assertTrue(appointment.getDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("收集并清除领域事件")
    void pollAndClearEvents_ReturnsEventsAndClearsThem() {
        // given
        Appointment appointment = createTestAppointment();

        // when
        List<Object> events = appointment.pollAndClearEvents();

        // then
        assertEquals(1, events.size());
        assertTrue(appointment.getDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("检查是否为终态")
    void isTerminal_ReturnsCorrectValue() {
        // given
        Appointment unpaidAppointment = createTestAppointment();
        Appointment paidAppointment = createTestAppointment();
        paidAppointment.markAsPaid();
        Appointment visitedAppointment = createTestAppointment();
        visitedAppointment.markAsPaid();
        visitedAppointment.markAsVisited();
        Appointment cancelledAppointment = createTestAppointment();
        cancelledAppointment.cancel("原因");

        // then
        assertFalse(unpaidAppointment.isTerminal());
        assertFalse(paidAppointment.isTerminal());
        assertTrue(visitedAppointment.isTerminal());
        assertTrue(cancelledAppointment.isTerminal());
    }

    @Test
    @DisplayName("状态变更事件包含正确信息")
    void statusChangedEvent_ContainsCorrectInfo() {
        // given
        Appointment appointment = createTestAppointment();
        appointment.pollAndClearEvents(); // 清除创建事件

        // when
        appointment.markAsPaid();

        // then
        List<Object> events = appointment.pollAndClearEvents();
        assertEquals(1, events.size());
        AppointmentStatusChangedEvent event = (AppointmentStatusChangedEvent) events.get(0);
        assertEquals(AppointmentStatus.UNPAID, event.oldStatus());
        assertEquals(AppointmentStatus.CONFIRMED, event.newStatus());
    }

    // ============ 辅助方法 ============

    private Appointment createTestAppointment() {
        return Appointment.create(
                PATIENT_ID,
                DOCTOR_ID,
                SCHEDULE_ID,
                APPT_DATE,
                TIME_PERIOD,
                APPT_TIME,
                APPT_FEE,
                "测试主诉",
                APPT_NO);
    }
}
