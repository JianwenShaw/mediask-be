package me.jianwen.mediask.common.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AiOverviewMetricsDTO {

    private LocalDate metricDate;
    private Long totalConversations;
    private Long activeUsers;
    private Long totalMessages;
    private Long totalReviews;
    private BigDecimal avgReviewScore;
    private BigDecimal accuracyRate;
}
