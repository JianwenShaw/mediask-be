package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.ConversationStatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI对话会话实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("ai_conversations")
public class AiConversationDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 业务会话唯一ID
     */
    private String conversationUuid;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 场景类型
     */
    private String sceneType;

    /**
     * 会话摘要
     */
    private String summary;

    /**
     * 状态
     */
    private ConversationStatusEnum status;

    /**
     * 开始时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime endedAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
