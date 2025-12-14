package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 连接测试表实体
 * 用于验证数据库连接是否正常
 */
@Data
@TableName("test_connections")
public class TestConnectionEntity {

    /**
     * 主键ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 测试消息
     */
    private String message;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
