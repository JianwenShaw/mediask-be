package me.jianwen.mediask.schedule.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.infra.lock.annotation.DistributedLockable;
import me.jianwen.mediask.schedule.application.dto.AppointmentDTO;
import me.jianwen.mediask.schedule.application.dto.AppointmentResultDTO;
import me.jianwen.mediask.schedule.application.dto.AvailableSlotDTO;
import me.jianwen.mediask.schedule.application.request.CancelAppointmentRequest;
import me.jianwen.mediask.schedule.application.request.CreateAppointmentRequest;
import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.repository.AppointmentRepository;
import me.jianwen.mediask.schedule.domain.repository.AppointmentSlotRepository;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.service.SlotManagementDomainService;
import me.jianwen.mediask.schedule.domain.valueobject.*;
import me.jianwen.mediask.infra.event.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预约挂号应用服务
 *
 * 职责：
 * 1. 协调领域服务完成预约业务用例
 * 2. 管理事务边界
 * 3. 发布领域事件
 * 4. Request 到领域对象的转换
 *
 * @author jianwen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentApplicationService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final AppointmentSlotRepository slotRepository;
    private final SlotManagementDomainService slotManagementDomainService;
    private final DomainEventPublisher eventPublisher;

    /**
     * 创建预约
     */
    @DistributedLockable(
            key = "'appt:create:' + #request.scheduleId + ':' + #request.apptTime",
            waitTime = 3,
            leaseTime = 30,
            errorMessage = "号源正在被锁定，请稍后重试"
    )
    @Transactional(rollbackFor = Exception.class)
    public AppointmentResultDTO createAppointment(Long patientId, CreateAppointmentRequest request) {
        log.info("创建预约: patientId={}, scheduleId={}, date={}, time={}",
                patientId, request.getScheduleId(), request.getApptDate(), request.getApptTime());

        ScheduleId scheduleId = ScheduleId.of(request.getScheduleId());
        PatientId patientIdVO = PatientId.of(patientId);
        TimePeriod timePeriod = TimePeriod.fromCode(request.getTimePeriodCode());

        // 获取排班信息
        DoctorSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BizException(ErrorCode.SCHEDULE_NOT_FOUND, "排班记录不存在"));

        // 业务规则校验
        if (!canMakeAppointment(schedule, request.getApptDate(), timePeriod)) {
            throw new BizException(ErrorCode.SCHEDULE_UNAVAILABLE, "当前排班不可预约");
        }

        // 检查是否可预约（时段冲突）
        if (appointmentRepository.existsByPatientIdAndDateAndTime(
                patientIdVO, request.getApptDate(), request.getApptTime())) {
            throw new BizException(ErrorCode.APPT_TIME_CONFLICT, "该时间段您已有挂号记录");
        }

        // 获取可用时段
        AppointmentSlot slot = slotRepository.findByScheduleAndTime(scheduleId, request.getApptTime())
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "号源不存在"));

        if (!slot.isAvailable()) {
            throw new BizException(ErrorCode.APPT_NO_SLOTS, "该时段号源已被占用");
        }

        // 生成预约单号
        String apptNo = generateApptNo();

        // 双重检查时段可用性
        slot = slotRepository.findByScheduleAndTime(scheduleId, request.getApptTime())
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "号源不存在"));

        if (!slot.isAvailable()) {
            throw new BizException(ErrorCode.APPT_NO_SLOTS, "该时段号源已挂完");
        }

        // 创建预约
        Appointment appointment = Appointment.create(
                patientIdVO,
                schedule.getDoctorId(),
                scheduleId,
                request.getApptDate(),
                timePeriod,
                request.getApptTime(),
                BigDecimal.valueOf(50), // TODO: 从排班配置获取挂号费
                request.getChiefComplaint(),
                apptNo);

        // 保存预约
        appointmentRepository.save(appointment);

        // 占用号源
        slotManagementDomainService.occupySlot(slot.getId(), appointment.getId().value());

        // 扣减排班号源
        schedule.decreaseSlot();
        scheduleRepository.save(schedule);

        // 发布领域事件
        publishEvents(appointment);

        log.info("预约创建成功: appointmentId={}, apptNo={}", appointment.getId(), apptNo);

        return AppointmentResultDTO.builder()
                .appointmentId(appointment.getId().value())
                .apptNo(apptNo)
                .doctorId(schedule.getDoctorId().getValue())
                .doctorName(null) // 需要通过医生服务获取
                .departmentName(null)
                .apptDate(request.getApptDate())
                .timePeriodDesc(timePeriod.getDescription())
                .apptTime(request.getApptTime())
                .status("待支付")
                .apptFee(BigDecimal.valueOf(50)) // TODO: 从排班配置获取挂号费
                .payDeadline(LocalDateTime.now().plusMinutes(30)) // 30分钟支付时限
                .build();
    }

    /**
     * 取消预约
     */
    @DistributedLockable(
            key = "'appt:cancel:' + #request.appointmentId",
            waitTime = 3,
            leaseTime = 10,
            errorMessage = "系统繁忙，请稍后重试"
    )
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppointment(Long patientId, CancelAppointmentRequest request) {
        log.info("取消预约: patientId={}, appointmentId={}", patientId, request.getAppointmentId());

        AppointmentId appointmentId = AppointmentId.of(request.getAppointmentId());

        // 获取预约
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BizException(ErrorCode.APPT_NOT_FOUND, "预约记录不存在"));

        // 验证预约归属
        if (!appointment.getPatientId().value().equals(patientId)) {
            throw new BizException(ErrorCode.EMR_ACCESS_DENIED, "无权取消该预约");
        }

        // 检查是否可取消
        if (!appointment.canCancel()) {
            throw new BizException(ErrorCode.APPT_CANCEL_FAILED,
                    "当前状态不允许取消: " + getStatusDescription(appointment.getStatus()));
        }

        // 取消预约
        appointment.cancel(request.getReason());
        appointmentRepository.save(appointment);

        // 释放号源
        List<Appointment> appointments = appointmentRepository.findByScheduleId(appointment.getScheduleId());
        appointments.stream()
                .filter(a -> a.getApptTime().equals(appointment.getApptTime()) && !a.isCancelled())
                .findFirst()
                .ifPresentOrElse(
                        a -> slotManagementDomainService.occupySlot(
                                slotRepository.findByScheduleAndTime(appointment.getScheduleId(), appointment.getApptTime())
                                        .map(AppointmentSlot::getId)
                                        .orElse(null),
                                a.getId().value()),
                        () -> {
                            // 没有其他预约占用该时段，释放号源
                            slotRepository.findByScheduleAndTime(appointment.getScheduleId(), appointment.getApptTime())
                                    .ifPresent(slot -> {
                                        slot.release();
                                        slotRepository.save(slot);
                                    });
                        });

        // 恢复排班号源
        DoctorSchedule schedule = scheduleRepository.findById(appointment.getScheduleId())
                .orElseThrow(() -> new BizException(ErrorCode.SCHEDULE_NOT_FOUND));
        schedule.increaseSlot();
        scheduleRepository.save(schedule);

        // 发布事件
        publishEvents(appointment);

        log.info("预约取消成功: appointmentId={}", appointmentId.value());
    }

    /**
     * 支付预约
     */
    @Transactional(rollbackFor = Exception.class)
    public void payAppointment(Long appointmentId) {
        log.info("支付预约: appointmentId={}", appointmentId);

        AppointmentId id = AppointmentId.of(appointmentId);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.APPT_NOT_FOUND, "预约记录不存在"));

        if (!appointment.canPay()) {
            throw new BizException(ErrorCode.APPT_STATUS_ERROR, "当前状态不允许支付");
        }

        appointment.markAsPaid();
        appointmentRepository.save(appointment);

        publishEvents(appointment);

        log.info("预约支付成功: appointmentId={}", appointmentId);
    }

    /**
     * 标记已就诊
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsVisited(Long appointmentId) {
        log.info("标记就诊: appointmentId={}", appointmentId);

        AppointmentId id = AppointmentId.of(appointmentId);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.APPT_NOT_FOUND, "预约记录不存在"));

        appointment.markAsVisited();
        appointmentRepository.save(appointment);

        publishEvents(appointment);

        log.info("标记就诊成功: appointmentId={}", appointmentId);
    }

    /**
     * 查询患者预约列表
     */
    public List<AppointmentDTO> listPatientAppointments(Long patientId, LocalDate startDate, LocalDate endDate) {
        PatientId patientIdVO = PatientId.of(patientId);
        List<Appointment> appointments;

        if (startDate != null && endDate != null) {
            appointments = appointmentRepository.findByPatientIdAndDateRange(patientIdVO, startDate, endDate);
        } else {
            appointments = appointmentRepository.findByPatientId(patientIdVO);
        }

        return appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询患者待支付预约
     */
    public List<AppointmentDTO> listUnpaidAppointments(Long patientId) {
        PatientId patientIdVO = PatientId.of(patientId);
        List<Appointment> appointments = appointmentRepository.findByPatientIdAndStatus(
                patientIdVO, AppointmentStatus.UNPAID);
        return appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询可预约时段
     */
    public List<AvailableSlotDTO> listAvailableSlots(Long scheduleId) {
        ScheduleId id = ScheduleId.of(scheduleId);
        List<AppointmentSlot> slots = slotRepository.findBySchedule(id);

        return slots.stream()
                .filter(AppointmentSlot::isAvailable)
                .map(slot -> AvailableSlotDTO.builder()
                        .slotId(slot.getId())
                        .scheduleId(scheduleId)
                        .time(slot.getTimeSlot().getStartTime())
                        .available(true)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 查询预约详情
     */
    public AppointmentDTO getAppointment(Long appointmentId) {
        AppointmentId id = AppointmentId.of(appointmentId);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.APPT_NOT_FOUND, "预约记录不存在"));
        return convertToDTO(appointment);
    }

    // ============ 私有方法 ============

    /**
     * 检查是否可预约
     */
    private boolean canMakeAppointment(DoctorSchedule schedule, LocalDate apptDate, TimePeriod timePeriod) {
        // 检查日期是否有效
        if (apptDate.isBefore(LocalDate.now())) {
            return false;
        }

        // 检查排班状态
        if (!schedule.getStatus().canAppointment()) {
            return false;
        }

        // 检查号源是否充足
        if (!schedule.getCapacity().hasAvailable()) {
            return false;
        }

        // 检查日期是否在排班日期范围内
        if (!apptDate.equals(schedule.getScheduleDate())) {
            return false;
        }

        // 检查时段是否匹配
        return schedule.getTimePeriod() == timePeriod;
    }

    private String generateApptNo() {
        int random = (int) (Math.random() * 10000);
        return "APPT" + System.currentTimeMillis() + String.format("%04d", random);
    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        DoctorSchedule schedule = null;
        if (appointment.getScheduleId() != null) {
            schedule = scheduleRepository.findById(appointment.getScheduleId()).orElse(null);
        }

        return AppointmentDTO.builder()
                .id(appointment.getId() != null ? appointment.getId().value() : null)
                .apptNo(appointment.getApptNo())
                .patientId(appointment.getPatientId().value())
                .doctorId(appointment.getDoctorId().getValue())
                .scheduleId(appointment.getScheduleId() != null ? appointment.getScheduleId().getValue() : null)
                .apptDate(appointment.getApptDate())
                .timePeriodDesc(appointment.getTimePeriod().getDescription())
                .apptTime(appointment.getApptTime())
                .statusCode(appointment.getStatus().code())
                .statusDesc(appointment.getStatus().description())
                .chiefComplaint(appointment.getChiefComplaint())
                .apptFee(appointment.getApptFee())
                .paidAt(appointment.getPaidAt())
                .visitedAt(appointment.getVisitedAt())
                .createdAt(appointment.getCreatedAt())
                .build();
    }

    private String getStatusDescription(AppointmentStatus status) {
        return switch (status.code()) {
            case 1 -> "待支付";
            case 2 -> "已预约";
            case 3 -> "已就诊";
            case 4 -> "已取消";
            case 5 -> "爽约";
            default -> "未知";
        };
    }

    private void publishEvents(Appointment appointment) {
        appointment.getDomainEvents().forEach(event -> {
            if (event instanceof me.jianwen.mediask.schedule.domain.event.AppointmentCreatedEvent createdEvent) {
                eventPublisher.publishAppointmentCreated(createdEvent);
            } else if (event instanceof me.jianwen.mediask.schedule.domain.event.AppointmentStatusChangedEvent statusEvent) {
                eventPublisher.publishAppointmentStatusChanged(statusEvent);
            } else {
                log.warn("未处理的领域事件类型: {}", event.getClass().getName());
            }
        });
        appointment.clearDomainEvents();
    }
}
