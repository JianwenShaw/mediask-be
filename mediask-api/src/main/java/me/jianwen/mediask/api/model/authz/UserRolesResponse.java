package me.jianwen.mediask.api.model.authz;

import lombok.Data;

import java.util.List;

@Data
public class UserRolesResponse {

    private Long userId;
    private List<String> roleCodes;
}
