package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("schedule_plan")
public class SchedulePlanDO implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String planCode;
    private Long departmentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer versionNo;
    private String planStatus;
    private String solverStrategy;
    private Long generatedBy;
    private BigDecimal totalScore;
    private Integer hardViolationCount;
    private String warningsJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
