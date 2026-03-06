package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.mapper.config.MapStructConfig;
import me.jianwen.mediask.api.request.doctor.CreateDoctorRequest;
import me.jianwen.mediask.api.request.doctor.UpdateDoctorRequest;
import me.jianwen.mediask.api.response.doctor.DoctorResponse;
import me.jianwen.mediask.service.application.command.CreateDoctorCommand;
import me.jianwen.mediask.service.application.command.UpdateDoctorCommand;
import me.jianwen.mediask.service.application.dto.doctor.DoctorDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface DoctorApiMapper {

    CreateDoctorCommand toService(CreateDoctorRequest request);

    UpdateDoctorCommand toService(UpdateDoctorRequest request);

    DoctorResponse toResponse(DoctorDTO doctorDTO);
}
