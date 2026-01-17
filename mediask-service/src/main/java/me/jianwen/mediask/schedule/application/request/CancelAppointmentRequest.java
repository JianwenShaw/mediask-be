package me.jianwen.mediask.schedule.application.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 取消预约请求
 *
 * @author jianwen
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
}
