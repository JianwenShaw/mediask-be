package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.ParseStatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("knowledge_documents")
public class KnowledgeDocumentDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 文档类型
     */
    private String docType;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 解析状态
     */
    private ParseStatusEnum parseStatus;

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

    /**
     * 软删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
