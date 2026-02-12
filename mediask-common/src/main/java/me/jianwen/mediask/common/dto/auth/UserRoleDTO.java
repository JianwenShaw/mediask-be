package me.jianwen.mediask.common.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserRoleDTO {

    private Long userId;
    private List<String> roleCodes;
}
