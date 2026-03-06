package me.jianwen.mediask.schedule.domain.readmodel;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AiDepartmentMetrics {

    private LocalDate metricDate;
    private Long departmentId;
    private String departmentName;
    private Long totalConversations;
    private Long totalReviews;
    private BigDecimal avgReviewScore;
    private BigDecimal accuracyRate;
}
