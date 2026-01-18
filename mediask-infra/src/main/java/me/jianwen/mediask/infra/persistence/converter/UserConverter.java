package me.jianwen.mediask.infra.persistence.converter;

import me.jianwen.mediask.dal.entity.UserDO;
import me.jianwen.mediask.dal.enums.GenderEnum;
import me.jianwen.mediask.dal.enums.UserTypeEnum;
import me.jianwen.mediask.user.domain.entity.User;
import me.jianwen.mediask.user.domain.enums.Gender;
import me.jianwen.mediask.user.domain.enums.UserType;
import org.springframework.stereotype.Component;

/**
 * 用户转换器
 *
 * @author jianwen
 */
@Component
public class UserConverter {

    /**
     * DO 转领域实体
     */
    public User toDomain(UserDO userDO) {
        if (userDO == null) {
            return null;
        }
        return User.builder()
                .id(userDO.getId())
                .username(userDO.getUsername())
                .phone(userDO.getPhone())
                .password(userDO.getPassword())
                .userType(convertUserType(userDO.getUserType()))
                .realName(userDO.getRealName())
                .gender(convertGender(userDO.getGender()))
                .birthDate(userDO.getBirthDate())
                .avatarUrl(userDO.getAvatarUrl())
                .build();
    }

    /**
     * 领域实体转 DO
     */
    public UserDO toDataObject(User user) {
        if (user == null) {
            return null;
        }
        UserDO userDO = new UserDO();
        userDO.setId(user.getId());
        userDO.setUsername(user.getUsername());
        userDO.setPhone(user.getPhone());
        userDO.setPassword(user.getPassword());
        userDO.setUserType(convertUserTypeEnum(user.getUserType()));
        userDO.setRealName(user.getRealName());
        userDO.setGender(convertGenderEnum(user.getGender()));
        userDO.setBirthDate(user.getBirthDate());
        userDO.setAvatarUrl(user.getAvatarUrl());
        return userDO;
    }

    private UserType convertUserType(UserTypeEnum userTypeEnum) {
        if (userTypeEnum == null) {
            return null;
        }
        return UserType.fromCode(userTypeEnum.getCode());
    }

    private UserTypeEnum convertUserTypeEnum(UserType userType) {
        if (userType == null) {
            return null;
        }
        return UserTypeEnum.fromCode(userType.code());
    }

    private Gender convertGender(GenderEnum genderEnum) {
        if (genderEnum == null) {
            return null;
        }
        return Gender.fromCode(genderEnum.getCode());
    }

    private GenderEnum convertGenderEnum(Gender gender) {
        if (gender == null) {
            return null;
        }
        return GenderEnum.fromCode(gender.code());
    }
}
