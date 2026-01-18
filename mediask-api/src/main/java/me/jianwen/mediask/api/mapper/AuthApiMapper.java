package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.model.auth.LoginRequest;
import me.jianwen.mediask.api.model.auth.LoginResponse;
import me.jianwen.mediask.api.model.auth.RegisterRequest;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface AuthApiMapper {

    me.jianwen.mediask.service.application.command.LoginCommand toService(LoginRequest request);

    me.jianwen.mediask.service.application.command.RegisterCommand toService(RegisterRequest request);

    LoginResponse toResponse(me.jianwen.mediask.service.application.response.LoginResponse dto);
}
