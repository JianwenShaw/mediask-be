package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计日志实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("audit_logs")
public class AuditLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作类型
     */
    private String action;

    /**
     * 资源类型
     */
    private String resourceType;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * 修改前数据(快照JSON)
     */
    private String oldValue;

    /**
     * 修改后数据(快照JSON)
     */
    private String newValue;

    /**
     * 操作IP地址
     */
    private String ipAddress;

    /**
     * 用户代理(浏览器信息)
     */
    private String userAgent;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
