package me.jianwen.mediask.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.plan.SchedulePlan;
import me.jianwen.mediask.schedule.domain.plan.SchedulePlanItem;
import me.jianwen.mediask.schedule.domain.port.ScheduleDomainEventPublisher;
import me.jianwen.mediask.schedule.domain.repository.AppointmentRepository;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.repository.SchedulePlanRepository;
import me.jianwen.mediask.schedule.domain.service.SlotManagementDomainService;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
import me.jianwen.mediask.user.domain.repository.DoctorRepository;
import me.jianwen.mediask.service.application.dto.schedule.SchedulePlanPrecheckResultDTO;
import me.jianwen.mediask.service.application.dto.schedule.SchedulePlanPublishResultDTO;
import me.jianwen.mediask.service.application.dto.schedule.SchedulePlanVersionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulePlanApplicationService {

    private static final int DEFAULT_SLOT_DURATION_MINUTES = 15;
    private static final int DEFAULT_TOTAL_SLOTS_PER_SCHEDULE = 20;

    private final SchedulePlanRepository schedulePlanRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final SlotManagementDomainService slotManagementDomainService;
    private final DoctorRepository doctorRepository;
    private final ScheduleDomainEventPublisher eventPublisher;

    public List<SchedulePlanVersionDTO> listVersions(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "planCode不能为空");
        }
        return schedulePlanRepository.listByPlanCode(planCode).stream()
                .map(plan -> new SchedulePlanVersionDTO(
                        plan.id(),
                        plan.planCode(),
                        plan.versionNo(),
                        plan.planStatus(),
                        plan.startDate(),
                        plan.endDate(),
                        plan.totalScore(),
                        plan.hardViolationCount()
                ))
                .toList();
    }

    public SchedulePlanPrecheckResultDTO precheck(Long planId, String mode) {
        PublishMode publishMode = PublishMode.from(mode);
        PlanApplyContext context = buildPlanApplyContext(planId);
        int toCloseSchedules = (int) context.existingSchedules().stream()
                .filter(schedule -> {
                    SlotDoctorKey key = new SlotDoctorKey(
                            schedule.getScheduleDate(),
                            schedule.getTimePeriod().getCode(),
                            schedule.getDoctorId().getValue()
                    );
                    if (context.plannedKeys().contains(key)) {
                        return false;
                    }
                    return !hasActiveAppointments(schedule) && schedule.getStatus().getCode() != 0;
                })
                .count();
        int toCreateSchedules = (int) context.items().stream()
                .filter(item -> scheduleRepository.findByDoctorAndDateAndPeriod(
                        DoctorId.of(item.doctorId()),
                        item.scheduleDate(),
                        resolveTimePeriod(item.periodCode())
                ).isEmpty())
                .count();

        return new SchedulePlanPrecheckResultDTO(
                context.plan().id(),
                context.plan().planCode(),
                context.plan().versionNo(),
                publishMode.name(),
                publishMode == PublishMode.STRICT && !context.conflicts().isEmpty(),
                context.conflicts().size(),
                toCreateSchedules,
                toCloseSchedules,
                context.conflicts()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public SchedulePlanPublishResultDTO publish(Long planId, String mode) {
        return applyPlan(planId, "PUBLISH", PublishMode.from(mode));
    }

    @Transactional(rollbackFor = Exception.class)
    public SchedulePlanPublishResultDTO rollback(Long planId, String mode) {
        return applyPlan(planId, "ROLLBACK", PublishMode.from(mode));
    }

    private SchedulePlanPublishResultDTO applyPlan(Long planId, String action, PublishMode mode) {
        PlanApplyContext context = buildPlanApplyContext(planId);
        SchedulePlan plan = context.plan();
        List<SchedulePlanItem> items = context.items();

        int closedCount = 0;
        int createdCount = 0;
        List<String> warnings = new ArrayList<>();
        if (!context.conflicts().isEmpty() && mode == PublishMode.STRICT) {
            throw new BizException(ErrorCode.OPERATION_FORBIDDEN,
                    "STRICT发布失败，检测到冲突: " + String.join("; ", context.conflicts()));
        }
        warnings.addAll(context.conflicts());

        schedulePlanRepository.archivePublishedPlans(plan.planCode());
        schedulePlanRepository.updatePlanStatus(planId, "PUBLISHED");

        for (Long doctorId : context.departmentDoctors()) {
            List<DoctorSchedule> existing = scheduleRepository.findByDoctorAndDateRange(
                    DoctorId.of(doctorId), plan.startDate(), plan.endDate());
            for (DoctorSchedule schedule : existing) {
                SlotDoctorKey key = new SlotDoctorKey(
                        schedule.getScheduleDate(),
                        schedule.getTimePeriod().getCode(),
                        doctorId
                );
                if (context.plannedKeys().contains(key)) {
                    continue;
                }
                if (hasActiveAppointments(schedule)) {
                    warnings.add("跳过停诊(存在有效预约): scheduleId=%d,mode=%s"
                            .formatted(schedule.getId().getValue(), mode.name()));
                    continue;
                }
                if (schedule.getStatus().getCode() == 0) {
                    continue;
                }
                schedule.close("方案发布替换");
                scheduleRepository.save(schedule);
                publishScheduleEvents(schedule);
                closedCount++;
            }
        }

        for (SchedulePlanItem item : items) {
            TimePeriod period = resolveTimePeriod(item.periodCode());
            DoctorId doctorId = DoctorId.of(item.doctorId());
            var existing = scheduleRepository.findByDoctorAndDateAndPeriod(doctorId, item.scheduleDate(), period);
            if (existing.isPresent()) {
                DoctorSchedule schedule = existing.get();
                if (schedule.getStatus().getCode() == 0 && !hasActiveAppointments(schedule)) {
                    schedule.open();
                    scheduleRepository.save(schedule);
                    publishScheduleEvents(schedule);
                }
                continue;
            }

            DoctorSchedule schedule = DoctorSchedule.create(
                    ScheduleId.generate(),
                    doctorId,
                    item.scheduleDate(),
                    period,
                    DEFAULT_TOTAL_SLOTS_PER_SCHEDULE,
                    DEFAULT_SLOT_DURATION_MINUTES
            );
            scheduleRepository.save(schedule);
            List<AppointmentSlot> slots = slotManagementDomainService.generateSlotsForSchedule(schedule);
            slotManagementDomainService.saveSlots(slots);
            publishScheduleEvents(schedule);
            createdCount++;
        }

        return new SchedulePlanPublishResultDTO(
                plan.id(),
                plan.planCode(),
                plan.versionNo(),
                action,
                createdCount,
                closedCount,
                warnings
        );
    }

    private boolean hasActiveAppointments(DoctorSchedule schedule) {
        List<Appointment> appointments = appointmentRepository.findByScheduleId(schedule.getId());
        return appointments.stream().anyMatch(appointment -> !appointment.isCancelled());
    }

    private PlanApplyContext buildPlanApplyContext(Long planId) {
        SchedulePlan plan = schedulePlanRepository.findById(planId)
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "排班方案不存在: " + planId));
        List<SchedulePlanItem> items = schedulePlanRepository.listPlanItems(planId);
        if (items.isEmpty()) {
            throw new BizException(ErrorCode.OPERATION_FORBIDDEN, "排班方案明细为空，无法发布");
        }
        Set<SlotDoctorKey> plannedKeys = new HashSet<>();
        for (SchedulePlanItem item : items) {
            plannedKeys.add(new SlotDoctorKey(item.scheduleDate(), item.periodCode(), item.doctorId()));
        }
        List<Long> departmentDoctors = doctorRepository.listActiveByDepartment(plan.departmentId(), null).stream()
                .map(profile -> profile.getDoctorId())
                .toList();
        List<DoctorSchedule> existingSchedules = new ArrayList<>();
        for (Long doctorId : departmentDoctors) {
            List<DoctorSchedule> existing = scheduleRepository.findByDoctorAndDateRange(
                    DoctorId.of(doctorId), plan.startDate(), plan.endDate());
            existingSchedules.addAll(existing);
        }
        List<String> conflicts = new ArrayList<>();
        for (DoctorSchedule schedule : existingSchedules) {
            SlotDoctorKey key = new SlotDoctorKey(
                    schedule.getScheduleDate(),
                    schedule.getTimePeriod().getCode(),
                    schedule.getDoctorId().getValue()
            );
            if (plannedKeys.contains(key)) {
                continue;
            }
            if (!hasActiveAppointments(schedule)) {
                continue;
            }
            conflicts.add("存在有效预约，无法替换: scheduleId=%d,date=%s,period=%d,doctorId=%d".formatted(
                    schedule.getId().getValue(),
                    schedule.getScheduleDate(),
                    schedule.getTimePeriod().getCode(),
                    schedule.getDoctorId().getValue()
            ));
        }
        return new PlanApplyContext(plan, items, plannedKeys, departmentDoctors, existingSchedules, conflicts);
    }

    private TimePeriod resolveTimePeriod(Integer periodCode) {
        try {
            return TimePeriod.fromCode(periodCode);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "非法时段编码: " + periodCode);
        }
    }

    private void publishScheduleEvents(DoctorSchedule schedule) {
        schedule.getDomainEvents().forEach(event -> {
            if (event instanceof me.jianwen.mediask.schedule.domain.event.ScheduleCreatedEvent createdEvent) {
                eventPublisher.publishScheduleCreated(createdEvent);
                return;
            }
            if (event instanceof me.jianwen.mediask.schedule.domain.event.ScheduleSlotDecreasedEvent decreasedEvent) {
                eventPublisher.publishScheduleSlotDecreased(decreasedEvent);
                return;
            }
            if (event instanceof me.jianwen.mediask.schedule.domain.event.ScheduleSlotIncreasedEvent increasedEvent) {
                eventPublisher.publishScheduleSlotIncreased(increasedEvent);
                return;
            }
            if (event instanceof me.jianwen.mediask.schedule.domain.event.ScheduleStatusChangedEvent statusEvent) {
                eventPublisher.publishScheduleStatusChanged(statusEvent);
                return;
            }
            log.warn("未处理的领域事件类型: {}", event.getClass().getName());
        });
        schedule.clearDomainEvents();
    }

    private record SlotDoctorKey(LocalDate date, Integer periodCode, Long doctorId) {
    }

    private record PlanApplyContext(
            SchedulePlan plan,
            List<SchedulePlanItem> items,
            Set<SlotDoctorKey> plannedKeys,
            List<Long> departmentDoctors,
            List<DoctorSchedule> existingSchedules,
            List<String> conflicts
    ) {
    }

    private enum PublishMode {
        STRICT,
        FORCE;

        private static PublishMode from(String mode) {
            if (mode == null || mode.isBlank()) {
                return STRICT;
            }
            try {
                return PublishMode.valueOf(mode.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BizException(ErrorCode.PARAM_INVALID, "非法发布模式: " + mode + "，仅支持 STRICT/FORCE");
            }
        }
    }
}
