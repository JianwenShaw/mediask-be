package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.model.auth.LoginRequest;
import me.jianwen.mediask.api.model.auth.RegisterRequest;
import me.jianwen.mediask.service.application.command.LoginCommand;
import me.jianwen.mediask.service.application.command.RegisterCommand;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface AuthApiMapper {

    LoginCommand toService(LoginRequest request);

    RegisterCommand toService(RegisterRequest request);
}
