package me.jianwen.mediask.schedule.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiFeedbackReview {

    private Long id;
    private Long conversationId;
    private Long doctorId;
    private Long departmentId;
    private Integer reviewScore;
    private Integer isAdopted;
    private String reviewComment;
    private LocalDateTime reviewedAt;
}
