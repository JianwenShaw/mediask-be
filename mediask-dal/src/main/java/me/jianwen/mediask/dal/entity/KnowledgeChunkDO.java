package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库分块实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("knowledge_chunks")
public class KnowledgeChunkDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 文本片段
     */
    private String chunkContent;

    /**
     * Milvus向量ID
     */
    private String vectorId;

    /**
     * 分块序号
     */
    private Integer chunkIndex;

    /**
     * 元数据(JSON)
     */
    private String metadata;

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
