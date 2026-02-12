package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.model.doctor.CreateDoctorRequest;
import me.jianwen.mediask.api.model.doctor.DoctorResponse;
import me.jianwen.mediask.api.model.doctor.UpdateDoctorRequest;
import me.jianwen.mediask.common.dto.doctor.DoctorDTO;
import me.jianwen.mediask.service.application.command.CreateDoctorCommand;
import me.jianwen.mediask.service.application.command.UpdateDoctorCommand;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface DoctorApiMapper {

    CreateDoctorCommand toService(CreateDoctorRequest request);

    UpdateDoctorCommand toService(UpdateDoctorRequest request);

    DoctorResponse toResponse(DoctorDTO doctorDTO);
}
