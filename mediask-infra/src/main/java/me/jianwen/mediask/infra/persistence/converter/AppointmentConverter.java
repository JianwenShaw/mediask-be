package me.jianwen.mediask.infra.persistence.converter;

import me.jianwen.mediask.dal.entity.AppointmentDO;
import me.jianwen.mediask.dal.enums.ApptStatusEnum;
import me.jianwen.mediask.dal.enums.TimePeriodEnum;
import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.valueobject.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 预约对象转换器
 *
 * @author jianwen
 */
@Mapper(componentModel = "spring")
public interface AppointmentConverter {

    AppointmentConverter INSTANCE = Mappers.getMapper(AppointmentConverter.class);

    /**
     * 领域对象 -> 数据对象
     */
    default AppointmentDO toDataObject(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        AppointmentDO dataObject = new AppointmentDO();
        if (appointment.getId() != null) {
            dataObject.setId(appointment.getId().value());
        }
        dataObject.setApptNo(appointment.getApptNo());
        if (appointment.getPatientId() != null) {
            dataObject.setPatientId(appointment.getPatientId().value());
        }
        if (appointment.getDoctorId() != null) {
            dataObject.setDoctorId(appointment.getDoctorId().getValue());
        }
        if (appointment.getScheduleId() != null) {
            dataObject.setScheduleId(appointment.getScheduleId().getValue());
        }
        dataObject.setApptDate(appointment.getApptDate());
        if (appointment.getTimePeriod() != null) {
            dataObject.setTimePeriod(TimePeriodEnum.fromCode(appointment.getTimePeriod().getCode()));
        }
        dataObject.setApptTime(appointment.getApptTime());
        if (appointment.getStatus() != null) {
            dataObject.setApptStatus(ApptStatusEnum.fromCode(appointment.getStatus().code()));
        }
        dataObject.setChiefComplaint(appointment.getChiefComplaint());
        if (appointment.getApptFee() != null) {
            dataObject.setApptFee(appointment.getApptFee());
        }
        dataObject.setPaidAt(appointment.getPaidAt());
        dataObject.setVisitedAt(appointment.getVisitedAt());
        dataObject.setCreatedAt(appointment.getCreatedAt());
        dataObject.setUpdatedAt(appointment.getUpdatedAt());

        return dataObject;
    }

    /**
     * 数据对象 -> 领域对象
     */
    default Appointment toDomain(AppointmentDO dataObject) {
        if (dataObject == null) {
            return null;
        }

        Appointment appointment = new Appointment();
        appointment.setId(AppointmentId.of(dataObject.getId()));
        appointment.setApptNo(dataObject.getApptNo());
        appointment.setPatientId(PatientId.of(dataObject.getPatientId()));
        appointment.setDoctorId(DoctorId.of(dataObject.getDoctorId()));
        if (dataObject.getScheduleId() != null) {
            appointment.setScheduleId(ScheduleId.of(dataObject.getScheduleId()));
        }
        appointment.setApptDate(dataObject.getApptDate());
        if (dataObject.getTimePeriod() != null) {
            appointment.setTimePeriod(TimePeriod.fromCode(dataObject.getTimePeriod().getCode()));
        }
        appointment.setApptTime(dataObject.getApptTime());
        if (dataObject.getApptStatus() != null) {
            appointment.setStatus(AppointmentStatus.fromCode(dataObject.getApptStatus().getCode()));
        }
        appointment.setChiefComplaint(dataObject.getChiefComplaint());
        appointment.setApptFee(dataObject.getApptFee());
        appointment.setPaidAt(dataObject.getPaidAt());
        appointment.setVisitedAt(dataObject.getVisitedAt());
        appointment.setCreatedAt(dataObject.getCreatedAt());
        appointment.setUpdatedAt(dataObject.getUpdatedAt());

        return appointment;
    }
}
