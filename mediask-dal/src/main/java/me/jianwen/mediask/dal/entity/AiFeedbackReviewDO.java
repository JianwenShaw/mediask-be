package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI问诊复核记录
 */
@Data
@TableName("ai_feedback_reviews")
public class AiFeedbackReviewDO implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long conversationId;
    private Long doctorId;
    private Long departmentId;
    private Integer reviewScore;
    private Integer isAdopted;
    private String reviewComment;
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
