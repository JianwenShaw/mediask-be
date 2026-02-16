package me.jianwen.mediask.schedule.domain.entity;

import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.schedule.domain.event.AppointmentCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.AppointmentStatusChangedEvent;
import me.jianwen.mediask.schedule.domain.valueobject.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 预约挂号聚合根
 *
 * 职责：
 * 1. 管理预约的基本信息（患者、医生、日期、时段）
 * 2. 控制预约状态的流转
 * 3. 发布领域事件
 *
 * 聚合设计原则：
 * - 外部只能通过ID引用Appointment
 * - 状态变更通过领域服务协调
 *
 * @author jianwen
 */
@Getter
@Setter
public class Appointment {

    /**
     * 预约ID
     */
    private AppointmentId id;

    /**
     * 预约单号
     */
    private String apptNo;

    /**
     * 患者ID
     */
    private PatientId patientId;

    /**
     * 医生ID
     */
    private DoctorId doctorId;

    /**
     * 排班ID
     */
    private ScheduleId scheduleId;

    /**
     * 就诊日期
     */
    private LocalDate apptDate;

    /**
     * 时段
     */
    private TimePeriod timePeriod;

    /**
     * 具体就诊时间
     */
    private LocalTime apptTime;

    /**
     * 预约状态
     */
    private AppointmentStatus status;

    /**
     * 主诉（AI生成）
     */
    private String chiefComplaint;

    /**
     * 挂号费
     */
    private BigDecimal apptFee;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 就诊时间
     */
    private LocalDateTime visitedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 领域事件集合
     */
    private List<Object> domainEvents = new ArrayList<>();

    // ============ 工厂方法 ============

    /**
     * 创建预约（患者挂号）
     *
     * @param patientId      患者ID
     * @param doctorId       医生ID
     * @param scheduleId     排班ID
     * @param apptDate       就诊日期
     * @param timePeriod     时段
     * @param apptTime       具体时间
     * @param apptFee        挂号费
     * @param chiefComplaint 主诉
     * @param apptNo         预约单号
     * @return 预约实体
     */
    public static Appointment create(
            PatientId patientId,
            DoctorId doctorId,
            ScheduleId scheduleId,
            LocalDate apptDate,
            TimePeriod timePeriod,
            LocalTime apptTime,
            BigDecimal apptFee,
            String chiefComplaint,
            String apptNo) {

        Appointment appointment = new Appointment();
        appointment.patientId = patientId;
        appointment.doctorId = doctorId;
        appointment.scheduleId = scheduleId;
        appointment.apptDate = apptDate;
        appointment.timePeriod = timePeriod;
        appointment.apptTime = apptTime;
        appointment.apptFee = apptFee;
        appointment.chiefComplaint = chiefComplaint;
        appointment.apptNo = apptNo;
        appointment.status = AppointmentStatus.UNPAID;
        appointment.createdAt = LocalDateTime.now();
        appointment.updatedAt = LocalDateTime.now();

        // 发布预约创建事件
        appointment.addDomainEvent(new AppointmentCreatedEvent(
                appointment.id,
                patientId,
                doctorId,
                apptDate,
                apptNo,
                apptFee,
                appointment.createdAt));

        return appointment;
    }

    // ============ 业务行为 ============

    /**
     * 支付成功
     *
     * @throws IllegalStateException 如果状态不允许支付
     */
    public void markAsPaid() {
        if (!canPay()) {
            throw new IllegalStateException(
                    String.format("预约状态[%s]不允许执行[支付]操作", status.description()));
        }

        AppointmentStatus oldStatus = this.status;
        this.status = AppointmentStatus.CONFIRMED;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 发布状态变更事件
        addDomainEvent(new AppointmentStatusChangedEvent(
                this.id, this.patientId, this.doctorId, oldStatus, this.status, this.updatedAt));
    }

    /**
     * 取消预约
     *
     * @param reason 取消原因
     * @throws IllegalStateException 如果状态不允许取消
     */
    public void cancel(String reason) {
        if (!canCancel()) {
            throw new IllegalStateException(
                    String.format("预约状态[%s]不允许执行[取消]操作", status.description()));
        }

        AppointmentStatus oldStatus = this.status;
        this.status = AppointmentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();

        // 发布状态变更事件
        addDomainEvent(new AppointmentStatusChangedEvent(
                this.id, this.patientId, this.doctorId, oldStatus, this.status, this.updatedAt));
    }

    /**
     * 标记已就诊
     *
     * @throws IllegalStateException 如果状态不允许标记就诊
     */
    public void markAsVisited() {
        if (!status.canMarkVisited()) {
            throw new IllegalStateException(
                    String.format("预约状态[%s]不允许执行[标记就诊]操作", status.description()));
        }

        AppointmentStatus oldStatus = this.status;
        this.status = AppointmentStatus.VISITED;
        this.visitedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 发布状态变更事件
        addDomainEvent(new AppointmentStatusChangedEvent(
                this.id, this.patientId, this.doctorId, oldStatus, this.status, this.updatedAt));
    }

    /**
     * 标记爽约
     *
     * @throws IllegalStateException 如果状态为终态
     */
    public void markAsAbsent() {
        if (status.isTerminal()) {
            throw new IllegalStateException(
                    String.format("预约状态[%s]不允许执行[标记爽约]操作", status.description()));
        }

        AppointmentStatus oldStatus = this.status;
        this.status = AppointmentStatus.ABSENT;
        this.updatedAt = LocalDateTime.now();

        // 发布状态变更事件
        addDomainEvent(new AppointmentStatusChangedEvent(
                this.id, this.patientId, this.doctorId, oldStatus, this.status, this.updatedAt));
    }

    /**
     * 检查是否可取消
     */
    public boolean canCancel() {
        return status.canCancel();
    }

    /**
     * 检查是否可支付
     */
    public boolean canPay() {
        return status.canPay();
    }

    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return status.isCancelled();
    }

    /**
     * 检查是否为终态
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    // ============ 领域事件管理 ============

    /**
     * 添加领域事件
     *
     * @param event 领域事件
     */
    private void addDomainEvent(Object event) {
        this.domainEvents.add(event);
    }

    /**
     * 获取领域事件列表（返回副本，防止外部修改）
     *
     * @return 领域事件列表
     */
    public List<Object> getDomainEvents() {
        return new ArrayList<>(this.domainEvents);
    }

    /**
     * 清除领域事件
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    /**
     * 收集并清除领域事件
     *
     * @return 领域事件列表
     */
    public List<Object> pollAndClearEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }
}
