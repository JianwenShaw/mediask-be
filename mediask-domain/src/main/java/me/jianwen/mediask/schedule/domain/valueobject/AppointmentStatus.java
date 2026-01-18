package me.jianwen.mediask.schedule.domain.valueobject;

/**
 * 预约状态值对象
 *
 * @author jianwen
 */
public record AppointmentStatus(Integer code, String description) {

    /**
     * 待支付
     */
    public static final AppointmentStatus UNPAID = new AppointmentStatus(1, "待支付");

    /**
     * 已预约（已支付/已确认）
     */
    public static final AppointmentStatus CONFIRMED = new AppointmentStatus(2, "已预约");

    /**
     * 已就诊
     */
    public static final AppointmentStatus VISITED = new AppointmentStatus(3, "已就诊");

    /**
     * 已取消
     */
    public static final AppointmentStatus CANCELLED = new AppointmentStatus(4, "已取消");

    /**
     * 爽约
     */
    public static final AppointmentStatus ABSENT = new AppointmentStatus(5, "爽约");

    public static AppointmentStatus fromCode(Integer code) {
        return switch (code) {
            case 1 -> UNPAID;
            case 2 -> CONFIRMED;
            case 3 -> VISITED;
            case 4 -> CANCELLED;
            case 5 -> ABSENT;
            default -> throw new IllegalArgumentException("无效的预约状态: " + code);
        };
    }

    public boolean canCancel() {
        return this == UNPAID || this == CONFIRMED;
    }

    public boolean canPay() {
        return this == UNPAID;
    }

    public boolean canMarkVisited() {
        return this == CONFIRMED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isTerminal() {
        return this == VISITED || this == CANCELLED || this == ABSENT;
    }
}
