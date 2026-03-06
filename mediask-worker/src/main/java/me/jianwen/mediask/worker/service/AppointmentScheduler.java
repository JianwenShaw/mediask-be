package me.jianwen.mediask.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.LockKeys;
import me.jianwen.mediask.infra.lock.DistributedLock;
import me.jianwen.mediask.infra.lock.DistributedLockFactory;
import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.port.ScheduleDomainEventPublisher;
import me.jianwen.mediask.schedule.domain.repository.AppointmentRepository;
import me.jianwen.mediask.schedule.domain.repository.AppointmentSlotRepository;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.service.AppointmentStateMachineDomainService;
import me.jianwen.mediask.schedule.domain.service.AppointmentTransitionContext;
import me.jianwen.mediask.schedule.domain.service.SlotManagementDomainService;
import me.jianwen.mediask.schedule.domain.statemachine.TransitionResult;
import me.jianwen.mediask.schedule.domain.valueobject.AppointmentEvent;
import me.jianwen.mediask.schedule.domain.valueobject.AppointmentStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 预约定时任务处理器
 *
 * 处理预约超时、爽约等定时任务
 *
 * @author jianwen
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentScheduler {

    private static final int BATCH_LIMIT = 200;
    private static final String APPOINTMENT_TIMEOUT_JOB = "appointment-timeout";
    private static final String APPOINTMENT_ABSENT_JOB = "appointment-absent-mark";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotRepository slotRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final SlotManagementDomainService slotManagementDomainService;
    private final AppointmentStateMachineDomainService appointmentStateMachineDomainService;
    private final ScheduleDomainEventPublisher eventPublisher;
    private final DistributedLockFactory distributedLockFactory;
    private final TransactionTemplate transactionTemplate;

    private static final int PAYMENT_TIMEOUT_MINUTES = 30;

    /**
     * 处理预约超时未支付
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void processAppointmentTimeout() {
        executeWithJobLock(APPOINTMENT_TIMEOUT_JOB, () -> {
            log.info("开始处理预约超时任务");

            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
            List<Appointment> unpaidAppointments = appointmentRepository.findUnpaidAppointmentsCreatedBefore(
                    cutoff, BATCH_LIMIT);

            long processedCount = 0;

            for (Appointment appointment : unpaidAppointments) {
                try {
                    Boolean stateChanged = transactionTemplate.execute(status -> {
                        TransitionResult<AppointmentStatus, AppointmentEvent> result = appointmentStateMachineDomainService.transit(
                                appointment,
                                AppointmentEvent.PAY_TIMEOUT_CANCEL,
                                AppointmentTransitionContext.forCancel(appointment, "支付超时，系统自动取消")
                        );
                        if (!result.stateChanged()) {
                            return false;
                        }
                        appointmentRepository.save(appointment);
                        releaseAppointmentSlotAndRestoreSchedule(appointment);
                        publishEvents(appointment);
                        return true;
                    });
                    if (!Boolean.TRUE.equals(stateChanged)) {
                        continue;
                    }
                    log.info("预约超时已自动取消: appointmentId={}, apptNo={}",
                            appointment.getId().value(), appointment.getApptNo());
                    processedCount++;
                } catch (Exception e) {
                    log.error("预约超时处理失败: appointmentId={}", appointment.getId().value(), e);
                }
            }

            log.info("预约超时任务完成: 共处理 {} 条", processedCount);
        });
    }

    /**
     * 标记爽约预约
     * 每日12点执行
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void markAbsentAppointments() {
        executeWithJobLock(APPOINTMENT_ABSENT_JOB, () -> {
            log.info("开始标记爽约预约任务");

            LocalDate today = LocalDate.now();
            List<Appointment> allAppointments = appointmentRepository.findByStatusAndApptDateBefore(
                    AppointmentStatus.CONFIRMED, today, BATCH_LIMIT);

            long markedCount = 0;
            for (Appointment appointment : allAppointments) {
                try {
                    TransitionResult<AppointmentStatus, AppointmentEvent> result = appointmentStateMachineDomainService.transit(
                            appointment,
                            AppointmentEvent.SYSTEM_MARK_ABSENT,
                            AppointmentTransitionContext.of(appointment)
                    );
                    if (!result.stateChanged()) {
                        continue;
                    }
                    appointmentRepository.save(appointment);
                    publishEvents(appointment);
                    log.info("已标记爽约: appointmentId={}, apptNo={}, apptDate={}",
                            appointment.getId().value(), appointment.getApptNo(), appointment.getApptDate());
                    markedCount++;
                } catch (Exception e) {
                    log.error("爽约标记失败: appointmentId={}", appointment.getId().value(), e);
                }
            }

            log.info("爽约标记任务完成: 共标记 {} 条", markedCount);
        });
    }

    private void executeWithJobLock(String jobName, Runnable task) {
        String lockKey = LockKeys.JOB_EXECUTE.buildKey(jobName);
        DistributedLock lock = distributedLockFactory.createLock(lockKey, -1, TimeUnit.SECONDS);
        boolean acquired = lock.tryLock(0, TimeUnit.SECONDS);
        if (!acquired) {
            log.debug("任务被其他实例执行，当前实例跳过: jobName={}, lockKey={}", jobName, lock.getLockKey());
            return;
        }
        try {
            task.run();
        } finally {
            lock.unlock();
            log.debug("任务锁释放完成: jobName={}", jobName);
        }
    }

    private void releaseAppointmentSlotAndRestoreSchedule(Appointment appointment) {
        AppointmentSlot slot = slotRepository.findByScheduleAndTime(appointment.getScheduleId(), appointment.getApptTime())
                .orElseThrow(() -> new IllegalStateException("号源不存在: scheduleId=" + appointment.getScheduleId().getValue()));
        slotManagementDomainService.releaseSlot(slot.getId(), appointment.getId().value());
        boolean increased = scheduleRepository.increaseAvailableSlots(appointment.getScheduleId());
        if (!increased) {
            throw new IllegalStateException("回补排班号源失败: scheduleId=" + appointment.getScheduleId().getValue());
        }
    }

    private void publishEvents(Appointment appointment) {
        for (Object event : appointment.getDomainEvents()) {
            if (event instanceof me.jianwen.mediask.schedule.domain.event.AppointmentCreatedEvent createdEvent) {
                me.jianwen.mediask.schedule.domain.event.AppointmentCreatedEvent normalizedEvent =
                        createdEvent.appointmentId() == null
                                ? new me.jianwen.mediask.schedule.domain.event.AppointmentCreatedEvent(
                                appointment.getId(),
                                createdEvent.patientId(),
                                createdEvent.doctorId(),
                                createdEvent.apptDate(),
                                createdEvent.apptNo(),
                                createdEvent.apptFee(),
                                createdEvent.createdAt())
                                : createdEvent;
                eventPublisher.publishAppointmentCreated(normalizedEvent);
                continue;
            }
            if (event instanceof me.jianwen.mediask.schedule.domain.event.AppointmentStatusChangedEvent statusEvent) {
                eventPublisher.publishAppointmentStatusChanged(statusEvent);
                continue;
            }
            log.warn("未处理的领域事件类型: {}", event.getClass().getName());
        }
        appointment.clearDomainEvents();
    }
}
