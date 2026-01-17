package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.AppointmentDO;
import me.jianwen.mediask.dal.enums.ApptStatusEnum;
import me.jianwen.mediask.dal.enums.TimePeriodEnum;
import me.jianwen.mediask.dal.mapper.AppointmentMapper;
import me.jianwen.mediask.infra.persistence.converter.AppointmentConverter;
import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.repository.AppointmentRepository;
import me.jianwen.mediask.schedule.domain.valueobject.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 预约仓储实现（基础设施层）
 *
 * @author jianwen
 */
@Repository
@RequiredArgsConstructor
public class AppointmentRepositoryImpl implements AppointmentRepository {

    private final AppointmentMapper appointmentMapper;
    private final AppointmentConverter appointmentConverter;

    @Override
    public void save(Appointment appointment) {
        AppointmentDO dataObject = appointmentConverter.toDataObject(appointment);

        if (dataObject.getId() == null) {
            appointmentMapper.insert(dataObject);
            // 回填ID
            if (appointment.getId() != null) {
                try {
                    java.lang.reflect.Field field = Appointment.class.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(appointment, AppointmentId.of(dataObject.getId()));
                } catch (Exception e) {
                    // 忽略
                }
            }
        } else {
            appointmentMapper.updateById(dataObject);
        }
    }

    @Override
    public Optional<Appointment> findById(AppointmentId appointmentId) {
        AppointmentDO dataObject = appointmentMapper.selectById(appointmentId.value());
        if (dataObject == null) {
            return Optional.empty();
        }
        return Optional.of(appointmentConverter.toDomain(dataObject));
    }

    @Override
    public Optional<Appointment> findByApptNo(String apptNo) {
        LambdaQueryWrapper<AppointmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentDO::getApptNo, apptNo);

        AppointmentDO dataObject = appointmentMapper.selectOne(wrapper);
        if (dataObject == null) {
            return Optional.empty();
        }
        return Optional.of(appointmentConverter.toDomain(dataObject));
    }

    @Override
    public List<Appointment> findByPatientId(PatientId patientId) {
        LambdaQueryWrapper<AppointmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentDO::getPatientId, patientId.value())
                .orderByDesc(AppointmentDO::getCreatedAt);

        return appointmentMapper.selectList(wrapper).stream()
                .map(appointmentConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByPatientIdAndStatus(PatientId patientId, AppointmentStatus status) {
        LambdaQueryWrapper<AppointmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentDO::getPatientId, patientId.value())
                .eq(AppointmentDO::getApptStatus, ApptStatusEnum.fromCode(status.code()))
                .orderByDesc(AppointmentDO::getCreatedAt);

        return appointmentMapper.selectList(wrapper).stream()
                .map(appointmentConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByPatientIdAndDateRange(PatientId patientId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AppointmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentDO::getPatientId, patientId.value())
                .ge(AppointmentDO::getApptDate, startDate)
                .le(AppointmentDO::getApptDate, endDate)
                .orderByDesc(AppointmentDO::getApptDate, AppointmentDO::getApptTime);

        return appointmentMapper.selectList(wrapper).stream()
                .map(appointmentConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByDoctorIdAndDate(DoctorId doctorId, LocalDate apptDate) {
        LambdaQueryWrapper<AppointmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentDO::getDoctorId, doctorId.getValue())
                .eq(AppointmentDO::getApptDate, apptDate)
                .orderByAsc(AppointmentDO::getApptTime);

        return appointmentMapper.selectList(wrapper).stream()
                .map(appointmentConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByDoctorIdAndDateAndTimePeriod(
            DoctorId doctorId, LocalDate apptDate, TimePeriod timePeriod) {

        LambdaQueryWrapper<AppointmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentDO::getDoctorId, doctorId.getValue())
                .eq(AppointmentDO::getApptDate, apptDate)
                .eq(AppointmentDO::getTimePeriod, TimePeriodEnum.fromCode(timePeriod.getCode()))
                .orderByAsc(AppointmentDO::getApptTime);

        return appointmentMapper.selectList(wrapper).stream()
                .map(appointmentConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPatientIdAndDateAndTimePeriod(
            PatientId patientId, LocalDate apptDate, TimePeriod timePeriod) {

        LambdaQueryWrapper<AppointmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentDO::getPatientId, patientId.value())
                .eq(AppointmentDO::getApptDate, apptDate)
                .eq(AppointmentDO::getTimePeriod, TimePeriodEnum.fromCode(timePeriod.getCode()));

        return appointmentMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByPatientIdAndDateAndTime(
            PatientId patientId, LocalDate apptDate, LocalTime apptTime) {

        LambdaQueryWrapper<AppointmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentDO::getPatientId, patientId.value())
                .eq(AppointmentDO::getApptDate, apptDate)
                .eq(AppointmentDO::getApptTime, apptTime);

        return appointmentMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long countByPatientIdAndDate(PatientId patientId, LocalDate apptDate) {
        LambdaQueryWrapper<AppointmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentDO::getPatientId, patientId.value())
                .eq(AppointmentDO::getApptDate, apptDate);
        return appointmentMapper.selectCount(wrapper);
    }

    @Override
    public List<Appointment> findByScheduleId(ScheduleId scheduleId) {
        LambdaQueryWrapper<AppointmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentDO::getScheduleId, scheduleId.getValue())
                .orderByDesc(AppointmentDO::getCreatedAt);

        return appointmentMapper.selectList(wrapper).stream()
                .map(appointmentConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(AppointmentId appointmentId) {
        appointmentMapper.deleteById(appointmentId.value());
    }
}
