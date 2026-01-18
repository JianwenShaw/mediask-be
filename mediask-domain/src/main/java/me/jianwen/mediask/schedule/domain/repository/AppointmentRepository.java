package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.valueobject.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 预约仓储接口
 *
 * @author jianwen
 */
public interface AppointmentRepository {

    /**
     * 保存预约
     */
    void save(Appointment appointment);

    /**
     * 根据ID查询
     */
    Optional<Appointment> findById(AppointmentId appointmentId);

    /**
     * 根据预约单号查询
     */
    Optional<Appointment> findByApptNo(String apptNo);

    /**
     * 查询患者的预约列表
     */
    List<Appointment> findByPatientId(PatientId patientId);

    /**
     * 查询患者的预约列表（按状态筛选）
     */
    List<Appointment> findByPatientIdAndStatus(PatientId patientId, AppointmentStatus status);

    /**
     * 查询患者的预约列表（日期范围）
     */
    List<Appointment> findByPatientIdAndDateRange(PatientId patientId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询医生的预约列表
     */
    List<Appointment> findByDoctorIdAndDate(DoctorId doctorId, LocalDate apptDate);

    /**
     * 查询医生在日期范围内的预约列表
     */
    List<Appointment> findByDoctorIdAndDateRange(DoctorId doctorId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询医生在某时段的预约
     */
    List<Appointment> findByDoctorIdAndDateAndTimePeriod(
            DoctorId doctorId, LocalDate apptDate, TimePeriod timePeriod);

    /**
     * 检查患者在同一时段是否有预约
     */
    boolean existsByPatientIdAndDateAndTimePeriod(
            PatientId patientId, LocalDate apptDate, TimePeriod timePeriod);

    /**
     * 检查患者在同一时间点是否有预约
     */
    boolean existsByPatientIdAndDateAndTime(
            PatientId patientId, LocalDate apptDate, LocalTime apptTime);

    /**
     * 统计患者在某日期的预约数量
     */
    long countByPatientIdAndDate(PatientId patientId, LocalDate apptDate);

    /**
     * 根据排班ID查询预约列表
     */
    List<Appointment> findByScheduleId(ScheduleId scheduleId);

    /**
     * 删除预约（物理删除，仅测试用）
     */
    void deleteById(AppointmentId appointmentId);
}
