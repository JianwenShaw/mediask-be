package me.jianwen.mediask.service.application.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserRoleDTO {

    private Long userId;
    private List<String> roleCodes;
}
