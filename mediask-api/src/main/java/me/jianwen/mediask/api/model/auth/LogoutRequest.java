package me.jianwen.mediask.api.model.auth;

import lombok.Data;

/**
 * 登出请求
 */
@Data
public class LogoutRequest {
    /**
     * Refresh Token ID（用于撤销当前设备）
     * 为空时撤销所有设备
     */
    private String refreshTokenId;
}
