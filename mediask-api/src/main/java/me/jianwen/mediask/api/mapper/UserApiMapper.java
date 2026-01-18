package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.model.user.CurrentUserResponse;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface UserApiMapper {

    CurrentUserResponse toResponse(me.jianwen.mediask.service.application.response.CurrentUserResponse dto);
}
