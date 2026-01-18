package me.jianwen.mediask.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.repository.AppointmentRepository;
import me.jianwen.mediask.schedule.domain.valueobject.AppointmentStatus;
import me.jianwen.mediask.schedule.domain.valueobject.PatientId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

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

    private final AppointmentRepository appointmentRepository;

    private static final int PAYMENT_TIMEOUT_MINUTES = 30;

    private static final int ABSENT_CHECK_HOUR = 12;

    /**
     * 处理预约超时未支付
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void processAppointmentTimeout() {
        log.info("开始处理预约超时任务");

        List<Appointment> unpaidAppointments = appointmentRepository.findByPatientIdAndStatus(
                PatientId.of(0L), AppointmentStatus.UNPAID);

        LocalDate today = LocalDate.now();
        long processedCount = 0;

        for (Appointment appointment : unpaidAppointments) {
            if (isAppointmentTimeout(appointment)) {
                try {
                    appointment.cancel("支付超时，系统自动取消");
                    appointmentRepository.save(appointment);
                    log.info("预约超时已自动取消: appointmentId={}, apptNo={}",
                            appointment.getId().value(), appointment.getApptNo());
                    processedCount++;
                } catch (Exception e) {
                    log.error("预约超时处理失败: appointmentId={}", appointment.getId().value(), e);
                }
            }
        }

        log.info("预约超时任务完成: 共处理 {} 条", processedCount);
    }

    /**
     * 标记爽约预约
     * 每日12点执行
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void markAbsentAppointments() {
        log.info("开始标记爽约预约任务");

        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<Appointment> allAppointments = appointmentRepository.findByPatientIdAndStatus(
                PatientId.of(0L), AppointmentStatus.CONFIRMED);

        long markedCount = 0;
        for (Appointment appointment : allAppointments) {
            if (appointment.getApptDate().isBefore(yesterday)) {
                try {
                    appointment.markAsAbsent();
                    appointmentRepository.save(appointment);
                    log.info("已标记爽约: appointmentId={}, apptNo={}, apptDate={}",
                            appointment.getId().value(), appointment.getApptNo(), appointment.getApptDate());
                    markedCount++;
                } catch (Exception e) {
                    log.error("爽约标记失败: appointmentId={}", appointment.getId().value(), e);
                }
            }
        }

        log.info("爽约标记任务完成: 共标记 {} 条", markedCount);
    }

    /**
     * 检查预约是否超时
     */
    private boolean isAppointmentTimeout(Appointment appointment) {
        if (appointment.getCreatedAt() == null) {
            return false;
        }

        if (!appointment.getApptDate().isAfter(LocalDate.now())) {
            return true;
        }

        return appointment.getCreatedAt()
                .plusMinutes(PAYMENT_TIMEOUT_MINUTES)
                .isBefore(java.time.LocalDateTime.now());
    }
}
