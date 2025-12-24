package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.MessageRoleEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI对话消息实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("ai_messages")
public class AiMessageDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 角色
     */
    private MessageRoleEnum role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * RAG检索上下文(JSON)
     */
    private String context;

    /**
     * 消耗Token数
     */
    private Integer tokensUsed;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 软删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
