package me.jianwen.mediask.user.domain.enums;

/**
 * 性别值对象
 *
 * @author jianwen
 */
public record Gender(Integer code, String description) {

    /**
     * 男
     */
    public static final Gender MALE = new Gender(1, "男");

    /**
     * 女
     */
    public static final Gender FEMALE = new Gender(2, "女");

    public static Gender fromCode(Integer code) {
        return switch (code) {
            case 1 -> MALE;
            case 2 -> FEMALE;
            default -> throw new IllegalArgumentException("无效的性别: " + code);
        };
    }
}
