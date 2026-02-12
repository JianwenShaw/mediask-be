package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.model.authz.PermissionResponse;
import me.jianwen.mediask.api.model.authz.RoleResponse;
import me.jianwen.mediask.api.model.authz.UpdateUserRolesRequest;
import me.jianwen.mediask.api.model.authz.UserRolesResponse;
import me.jianwen.mediask.common.dto.auth.PermissionDTO;
import me.jianwen.mediask.common.dto.auth.RoleDTO;
import me.jianwen.mediask.common.dto.auth.UserRoleDTO;
import me.jianwen.mediask.service.application.command.UpdateUserRolesCommand;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface AuthzApiMapper {

    RoleResponse toResponse(RoleDTO dto);

    PermissionResponse toResponse(PermissionDTO dto);

    UserRolesResponse toResponse(UserRoleDTO dto);

    UpdateUserRolesCommand toService(UpdateUserRolesRequest request);
}
