package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.mapper.config.MapStructConfig;
import me.jianwen.mediask.api.request.authz.UpdateUserRolesRequest;
import me.jianwen.mediask.api.response.authz.PermissionResponse;
import me.jianwen.mediask.api.response.authz.RoleResponse;
import me.jianwen.mediask.api.response.authz.UserRolesResponse;
import me.jianwen.mediask.service.application.command.UpdateUserRolesCommand;
import me.jianwen.mediask.service.application.dto.auth.PermissionDTO;
import me.jianwen.mediask.service.application.dto.auth.RoleDTO;
import me.jianwen.mediask.service.application.dto.auth.UserRoleDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface AuthzApiMapper {

    RoleResponse toResponse(RoleDTO dto);

    PermissionResponse toResponse(PermissionDTO dto);

    UserRolesResponse toResponse(UserRoleDTO dto);

    UpdateUserRolesCommand toService(UpdateUserRolesRequest request);
}
