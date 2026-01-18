package me.jianwen.mediask.user.domain.entity;

import lombok.Builder;
import lombok.Value;
import me.jianwen.mediask.user.domain.enums.Gender;
import me.jianwen.mediask.user.domain.enums.UserType;

import java.time.LocalDate;

/**
 * 用户领域实体
 */
@Value
@Builder
public class User {

    Long id;
    String username;
    String phone;
    String password;
    UserType userType;
    String realName;
    Gender gender;
    LocalDate birthDate;
    String avatarUrl;
}
