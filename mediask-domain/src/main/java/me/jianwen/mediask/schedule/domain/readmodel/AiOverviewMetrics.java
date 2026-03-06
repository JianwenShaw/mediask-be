package me.jianwen.mediask.schedule.domain.readmodel;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AiOverviewMetrics {

    private LocalDate metricDate;
    private Long totalConversations;
    private Long activeUsers;
    private Long totalMessages;
    private Long totalReviews;
    private BigDecimal avgReviewScore;
    private BigDecimal accuracyRate;
}
