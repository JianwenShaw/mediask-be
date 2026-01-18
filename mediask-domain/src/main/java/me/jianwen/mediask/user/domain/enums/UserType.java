package me.jianwen.mediask.user.domain.enums;

/**
 * 用户类型值对象
 *
 * @author jianwen
 */
public record UserType(Integer code, String description) {

    /**
     * 管理员
     */
    public static final UserType ADMIN = new UserType(1, "管理员");

    /**
     * 医生
     */
    public static final UserType DOCTOR = new UserType(2, "医生");

    /**
     * 患者
     */
    public static final UserType PATIENT = new UserType(3, "患者");

    public static UserType fromCode(Integer code) {
        return switch (code) {
            case 1 -> ADMIN;
            case 2 -> DOCTOR;
            case 3 -> PATIENT;
            default -> throw new IllegalArgumentException("无效的用户类型: " + code);
        };
    }
}
