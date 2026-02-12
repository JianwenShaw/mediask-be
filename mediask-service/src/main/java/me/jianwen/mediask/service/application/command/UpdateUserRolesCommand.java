package me.jianwen.mediask.service.application.command;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRolesCommand {

    private List<String> roleCodes;
}
