package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.model.appointment.AppointmentResponse;
import me.jianwen.mediask.api.model.appointment.AppointmentResultResponse;
import me.jianwen.mediask.api.model.appointment.AvailableSlotResponse;
import me.jianwen.mediask.api.model.appointment.CancelAppointmentRequest;
import me.jianwen.mediask.api.model.appointment.CreateAppointmentRequest;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface AppointmentApiMapper {

    me.jianwen.mediask.service.application.command.CreateAppointmentCommand toService(CreateAppointmentRequest request);

    me.jianwen.mediask.service.application.command.CancelAppointmentCommand toService(CancelAppointmentRequest request);

    AppointmentResponse toResponse(me.jianwen.mediask.service.application.response.AppointmentResponse dto);

    AppointmentResultResponse toResponse(me.jianwen.mediask.service.application.response.AppointmentResultResponse dto);

    AvailableSlotResponse toResponse(me.jianwen.mediask.service.application.response.AvailableSlotResponse dto);
}
