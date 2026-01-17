package me.jianwen.mediask.infra.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.event.AppointmentCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.AppointmentStatusChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 预约领域事件监听器
 *
 * 职责：
 * 1. 处理预约创建事件（发送通知、记录日志等）
 * 2. 处理预约状态变更事件（更新缓存、发送通知等）
 *
 * @author jianwen
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentEventListener {

    /**
     * 处理预约创建事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("接收到预约创建事件: appointmentId={}, patientId={}, doctorId={}, date={}",
                event.appointmentId().value(),
                event.patientId().value(),
                event.doctorId().getValue(),
                event.apptDate());

        try {
            // TODO: 发送预约成功通知（短信/公众号）
            // notificationService.sendAppointmentCreatedNotification(event);

            // TODO: 记录预约创建日志
            // appointmentLogService.logAppointmentCreated(event);

            // TODO: 更新用户预约统计
            // userStatisticsService.incrementAppointmentCount(event.patientId());

            log.info("预约创建事件处理完成: appointmentId={}", event.appointmentId().value());
        } catch (Exception e) {
            log.error("预约创建事件处理失败: appointmentId={}", event.appointmentId().value(), e);
        }
    }

    /**
     * 处理预约状态变更事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void handleAppointmentStatusChanged(AppointmentStatusChangedEvent event) {
        log.info("接收到预约状态变更事件: appointmentId={}, oldStatus={}, newStatus={}",
                event.appointmentId().value(),
                event.oldStatus().description(),
                event.newStatus().description());

        try {
            switch (event.newStatus().code()) {
                case 2 -> { // CONFIRMED - 已支付/已确认
                    log.info("预约已确认，发送确认通知: appointmentId={}", event.appointmentId().value());
                    // TODO: 发送预约确认通知
                    // notificationService.sendAppointmentConfirmedNotification(event);
                }
                case 3 -> { // VISITED - 已就诊
                    log.info("预约已完成就诊，更新就诊统计: appointmentId={}", event.appointmentId().value());
                    // TODO: 更新医生接诊统计
                    // doctorStatisticsService.incrementVisitCount(event.doctorId());
                }
                case 4 -> { // CANCELLED - 已取消
                    log.info("预约已取消，发送取消通知: appointmentId={}", event.appointmentId().value());
                    // TODO: 发送预约取消通知
                    // notificationService.sendAppointmentCancelledNotification(event);
                    // TODO: 触发退款流程（如已支付）
                    // refundService.processRefund(event.appointmentId());
                }
                case 5 -> { // ABSENT - 爽约
                    log.info("预约爽约，记录爽约统计: appointmentId={}", event.appointmentId().value());
                    // TODO: 更新用户爽约记录
                    // userAbsenceRecordService.recordAbsence(event.patientId(), event.doctorId());
                }
                default -> log.warn("未处理的状态变更: status={}", event.newStatus().code());
            }

            // TODO: 同步更新Redis缓存中的预约状态
            // cacheService.updateAppointmentStatus(event.appointmentId(), event.newStatus());

            log.info("预约状态变更事件处理完成: appointmentId={}, newStatus={}",
                    event.appointmentId().value(), event.newStatus().description());
        } catch (Exception e) {
            log.error("预约状态变更事件处理失败: appointmentId={}", event.appointmentId().value(), e);
        }
    }
}
