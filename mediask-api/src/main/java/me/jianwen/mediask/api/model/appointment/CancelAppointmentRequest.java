package me.jianwen.mediask.api.model.appointment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 取消预约请求（API层）
 */
@Data
public class CancelAppointmentRequest {

    /**
     * 预约ID
     */
    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;

    /**
     * 取消原因
     */
    private String reason;

    /**
     * 操作人ID（管理员取消时使用）
     */
    private Long operatorId;

    /**
     * 操作人类型：1-患者 2-管理员
     */
    private Integer operatorType;
}
