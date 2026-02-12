package me.jianwen.mediask.user.domain.enums;

/**
 * 用户类型值对象
 *
 * @author jianwen
 */
public record UserType(Integer code, String description) {

    /**
     * 患者
     */
    public static final UserType PATIENT = new UserType(1, "患者");

    /**
     * 医生
     */
    public static final UserType DOCTOR = new UserType(2, "医生");

    /**
     * 管理员
     */
    public static final UserType ADMIN = new UserType(3, "管理员");

    public static UserType fromCode(Integer code) {
        return switch (code) {
            case 1 -> PATIENT;
            case 2 -> DOCTOR;
            case 3 -> ADMIN;
            default -> throw new IllegalArgumentException("无效的用户类型: " + code);
        };
    }
}
