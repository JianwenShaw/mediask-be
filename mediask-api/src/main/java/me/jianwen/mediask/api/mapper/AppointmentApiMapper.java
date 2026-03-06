package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.mapper.config.MapStructConfig;
import me.jianwen.mediask.api.request.appointment.CancelAppointmentRequest;
import me.jianwen.mediask.api.request.appointment.CreateAppointmentRequest;
import me.jianwen.mediask.service.application.command.CancelAppointmentCommand;
import me.jianwen.mediask.service.application.command.CreateAppointmentCommand;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface AppointmentApiMapper {

    CreateAppointmentCommand toService(CreateAppointmentRequest request);

    CancelAppointmentCommand toService(CancelAppointmentRequest request);
}
