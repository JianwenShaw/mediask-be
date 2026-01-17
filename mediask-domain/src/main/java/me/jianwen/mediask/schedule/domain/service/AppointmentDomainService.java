package me.jianwen.mediask.schedule.domain.service;

import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.valueobject.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 预约挂号领域服务
 *
 * 职责：
 * 1. 处理跨聚合的业务逻辑
 * 2. 协调预约、排班、号源的交互
 *
 * @author jianwen
 */
public interface AppointmentDomainService {

    /**
     * 创建预约（核心业务方法）
     *
     * @param patientId      患者ID
     * @param doctorId       医生ID
     * @param scheduleId     排班ID
     * @param apptDate       就诊日期
     * @param timePeriod     时段
     * @param apptTime       具体时间
     * @param chiefComplaint 主诉
     * @param apptNo         预约单号
     * @return 预约实体
     */
    Appointment createAppointment(
            PatientId patientId,
            DoctorId doctorId,
            ScheduleId scheduleId,
            LocalDate apptDate,
            TimePeriod timePeriod,
            LocalTime apptTime,
            String chiefComplaint,
            String apptNo);

    /**
     * 确认预约号源（占用时段）
     *
     * @param scheduleId 排班ID
     * @param slotId     号源时段ID
     * @param appointmentId 预约ID
     */
    void confirmSlot(ScheduleId scheduleId, Long slotId, AppointmentId appointmentId);

    /**
     * 释放预约号源（取消预约时调用）
     *
     * @param scheduleId 排班ID
     * @param slotId     号源时段ID
     */
    void releaseSlot(ScheduleId scheduleId, Long slotId);

    /**
     * 取消预约
     *
     * @param appointmentId 预约ID
     * @param reason        取消原因
     */
    void cancelAppointment(AppointmentId appointmentId, String reason);

    /**
     * 支付预约
     *
     * @param appointmentId 预约ID
     */
    void payAppointment(AppointmentId appointmentId);

    /**
     * 获取可预约时段
     *
     * @param scheduleId 排班ID
     * @return 可用时段列表
     */
    java.util.List<AppointmentSlot> getAvailableSlots(ScheduleId scheduleId);

    /**
     * 检查是否可预约
     *
     * @param schedule    排班
     * @param patientId   患者ID
     * @param apptDate    就诊日期
     * @param timePeriod  时段
     * @param apptTime    具体时间
     * @return 是否可预约
     */
    boolean canMakeAppointment(
            DoctorSchedule schedule,
            PatientId patientId,
            LocalDate apptDate,
            TimePeriod timePeriod,
            LocalTime apptTime);

    /**
     * 获取排班信息
     *
     * @param scheduleId 排班ID
     * @return 排班实体
     */
    DoctorSchedule getSchedule(ScheduleId scheduleId);
}
