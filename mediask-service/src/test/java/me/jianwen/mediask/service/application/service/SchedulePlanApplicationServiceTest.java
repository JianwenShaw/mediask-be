package me.jianwen.mediask.service.application.service;

import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.dto.schedule.SchedulePlanPrecheckResultDTO;
import me.jianwen.mediask.common.dto.schedule.SchedulePlanPublishResultDTO;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.event.DomainEventPublisher;
import me.jianwen.mediask.domain.repository.DoctorRepository;
import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.optimization.model.SchedulePlan;
import me.jianwen.mediask.schedule.domain.optimization.model.SchedulePlanItem;
import me.jianwen.mediask.schedule.domain.repository.AppointmentRepository;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.repository.SchedulePlanRepository;
import me.jianwen.mediask.schedule.domain.service.SlotManagementDomainService;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleStatus;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
import me.jianwen.mediask.user.domain.entity.DoctorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulePlanApplicationServiceTest {

    @Mock
    private SchedulePlanRepository schedulePlanRepository;
    @Mock
    private DoctorScheduleRepository scheduleRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private SlotManagementDomainService slotManagementDomainService;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    private SchedulePlanApplicationService service;

    @BeforeEach
    void setUp() {
        service = new SchedulePlanApplicationService(
                schedulePlanRepository,
                scheduleRepository,
                appointmentRepository,
                slotManagementDomainService,
                doctorRepository,
                eventPublisher
        );
    }

    @Test
    void shouldBlockPrecheckInStrictModeWhenConflictExists() {
        prepareConflictContext();
        when(scheduleRepository.findByDoctorAndDateAndPeriod(any(), any(), any())).thenReturn(Optional.empty());

        SchedulePlanPrecheckResultDTO result = service.precheck(1L, "STRICT");

        assertTrue(result.wouldBlock());
        assertEquals(1, result.conflictCount());
        assertEquals(1, result.toCreateSchedules());
        assertEquals(0, result.toCloseSchedules());
    }

    @Test
    void shouldNotBlockPrecheckInForceModeWhenConflictExists() {
        prepareConflictContext();
        when(scheduleRepository.findByDoctorAndDateAndPeriod(any(), any(), any())).thenReturn(Optional.empty());

        SchedulePlanPrecheckResultDTO result = service.precheck(1L, "FORCE");

        assertFalse(result.wouldBlock());
        assertEquals(1, result.conflictCount());
    }

    @Test
    void shouldThrowAndNotUpdateStatusWhenStrictPublishHasConflict() {
        prepareConflictContext();

        BizException ex = assertThrows(BizException.class, () -> service.publish(1L, "STRICT"));
        assertEquals(ErrorCode.OPERATION_FORBIDDEN.getCode(), ex.getCode());
        verify(schedulePlanRepository, never()).archivePublishedPlans(any());
        verify(schedulePlanRepository, never()).updatePlanStatus(any(), any());
    }

    @Test
    void shouldContinuePublishInForceModeWhenConflictExists() {
        prepareConflictContext();
        when(scheduleRepository.findByDoctorAndDateAndPeriod(any(), any(), any())).thenReturn(Optional.empty());
        when(slotManagementDomainService.generateSlotsForSchedule(any())).thenReturn(List.of());

        SchedulePlanPublishResultDTO result = service.publish(1L, "FORCE");

        assertEquals(1, result.createdSchedules());
        assertEquals(0, result.closedSchedules());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("存在有效预约")));
        verify(schedulePlanRepository).archivePublishedPlans("AUTO-200-2026-02-17-2026-02-17");
        verify(schedulePlanRepository).updatePlanStatus(1L, "PUBLISHED");
    }

    @Test
    void shouldThrowWhenModeIsInvalid() {
        BizException ex = assertThrows(BizException.class, () -> service.precheck(1L, "UNKNOWN"));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    private void prepareConflictContext() {
        LocalDate date = LocalDate.of(2026, 2, 17);
        SchedulePlan plan = new SchedulePlan(
                1L,
                "AUTO-200-2026-02-17-2026-02-17",
                200L,
                date,
                date,
                1,
                "DRAFT",
                "HYBRID_V2",
                null,
                88.5D,
                0,
                "[]"
        );
        SchedulePlanItem item = new SchedulePlanItem(date, 1, 100L, false, "[]", "[]", null);
        DoctorProfile doctorProfile = DoctorProfile.builder()
                .doctorId(100L)
                .departmentId(200L)
                .title("主治医师")
                .build();

        DoctorSchedule existingSchedule = new DoctorSchedule();
        existingSchedule.setId(ScheduleId.of(9001L));
        existingSchedule.setDoctorId(DoctorId.of(100L));
        existingSchedule.setScheduleDate(date);
        existingSchedule.setTimePeriod(TimePeriod.AFTERNOON);
        existingSchedule.setStatus(ScheduleStatus.OPEN);

        Appointment activeAppointment = org.mockito.Mockito.mock(Appointment.class);
        when(activeAppointment.isCancelled()).thenReturn(false);

        when(schedulePlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(schedulePlanRepository.listPlanItems(1L)).thenReturn(List.of(item));
        when(doctorRepository.listActiveByDepartment(200L, null)).thenReturn(List.of(doctorProfile));
        when(scheduleRepository.findByDoctorAndDateRange(DoctorId.of(100L), date, date)).thenReturn(List.of(existingSchedule));
        when(appointmentRepository.findByScheduleId(ScheduleId.of(9001L))).thenReturn(List.of(activeAppointment));
    }
}
