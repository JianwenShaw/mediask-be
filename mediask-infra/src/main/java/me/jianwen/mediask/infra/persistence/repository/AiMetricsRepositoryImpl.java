package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.dto.ai.AiDepartmentMetricsDTO;
import me.jianwen.mediask.common.dto.ai.AiOverviewMetricsDTO;
import me.jianwen.mediask.dal.entity.AiConversationDO;
import me.jianwen.mediask.dal.entity.AiFeedbackReviewDO;
import me.jianwen.mediask.dal.entity.AiMessageDO;
import me.jianwen.mediask.dal.entity.DepartmentDO;
import me.jianwen.mediask.dal.mapper.AiConversationMapper;
import me.jianwen.mediask.dal.mapper.AiFeedbackReviewMapper;
import me.jianwen.mediask.dal.mapper.AiMessageMapper;
import me.jianwen.mediask.dal.mapper.DepartmentMapper;
import me.jianwen.mediask.schedule.domain.entity.AiFeedbackReview;
import me.jianwen.mediask.schedule.domain.repository.AiMetricsRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AiMetricsRepositoryImpl implements AiMetricsRepository {

    private final AiFeedbackReviewMapper reviewMapper;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    public void saveReview(AiFeedbackReview review) {
        AiFeedbackReviewDO reviewDO = new AiFeedbackReviewDO();
        reviewDO.setConversationId(review.getConversationId());
        reviewDO.setDoctorId(review.getDoctorId());
        reviewDO.setDepartmentId(review.getDepartmentId());
        reviewDO.setReviewScore(review.getReviewScore());
        reviewDO.setIsAdopted(review.getIsAdopted());
        reviewDO.setReviewComment(review.getReviewComment());
        reviewDO.setReviewedAt(review.getReviewedAt());
        reviewMapper.insert(reviewDO);
        review.setId(reviewDO.getId());
    }

    @Override
    public AiOverviewMetricsDTO getOverviewMetrics(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        Long totalConversations = conversationMapper.selectCount(new LambdaQueryWrapper<AiConversationDO>()
                .ge(AiConversationDO::getStartedAt, start)
                .lt(AiConversationDO::getStartedAt, end));

        List<Map<String, Object>> activeUserStats = conversationMapper.selectMaps(new QueryWrapper<AiConversationDO>()
                .select("COUNT(DISTINCT user_id) AS activeUsers")
                .ge("started_at", start)
                .lt("started_at", end));
        Long activeUsers = activeUserStats.isEmpty() ? 0L : toLong(activeUserStats.get(0).get("activeUsers"));

        Long totalMessages = messageMapper.selectCount(new LambdaQueryWrapper<AiMessageDO>()
                .ge(AiMessageDO::getCreatedAt, start)
                .lt(AiMessageDO::getCreatedAt, end));

        List<Map<String, Object>> stats = reviewMapper.selectMaps(new QueryWrapper<AiFeedbackReviewDO>()
                .select("COUNT(1) AS totalReviews", "AVG(review_score) AS avgReviewScore", "AVG(is_adopted) AS avgAdoptRate")
                .ge("reviewed_at", start)
                .lt("reviewed_at", end));

        long totalReviews = 0L;
        BigDecimal avgReviewScore = BigDecimal.ZERO;
        BigDecimal accuracyRate = BigDecimal.ZERO;
        if (!stats.isEmpty()) {
            Map<String, Object> map = stats.get(0);
            totalReviews = toLong(map.get("totalReviews"));
            avgReviewScore = toDecimal(map.get("avgReviewScore"));
            accuracyRate = toDecimal(map.get("avgAdoptRate")).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }

        return AiOverviewMetricsDTO.builder()
                .metricDate(date)
                .totalConversations(totalConversations)
                .activeUsers(activeUsers)
                .totalMessages(totalMessages)
                .totalReviews(totalReviews)
                .avgReviewScore(avgReviewScore.setScale(2, RoundingMode.HALF_UP))
                .accuracyRate(accuracyRate)
                .build();
    }

    @Override
    public List<AiDepartmentMetricsDTO> listDepartmentMetrics(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Map<String, Object>> rows = reviewMapper.selectMaps(new QueryWrapper<AiFeedbackReviewDO>()
                .select("department_id AS departmentId",
                        "COUNT(DISTINCT conversation_id) AS totalConversations",
                        "COUNT(1) AS totalReviews",
                        "AVG(review_score) AS avgReviewScore",
                        "AVG(is_adopted) AS avgAdoptRate")
                .ge("reviewed_at", start)
                .lt("reviewed_at", end)
                .groupBy("department_id"));

        List<AiDepartmentMetricsDTO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long departmentId = toLong(row.get("departmentId"));
            DepartmentDO department = departmentId != null ? departmentMapper.selectById(departmentId) : null;
            BigDecimal adoptRate = toDecimal(row.get("avgAdoptRate")).multiply(BigDecimal.valueOf(100));

            result.add(AiDepartmentMetricsDTO.builder()
                    .metricDate(date)
                    .departmentId(departmentId)
                    .departmentName(department != null ? department.getDeptName() : null)
                    .totalConversations(toLong(row.get("totalConversations")))
                    .totalReviews(toLong(row.get("totalReviews")))
                    .avgReviewScore(toDecimal(row.get("avgReviewScore")).setScale(2, RoundingMode.HALF_UP))
                    .accuracyRate(adoptRate.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return result;
    }

    private Long toLong(Object val) {
        if (val == null) {
            return 0L;
        }
        if (val instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(val.toString());
    }

    private BigDecimal toDecimal(Object val) {
        if (val == null) {
            return BigDecimal.ZERO;
        }
        if (val instanceof BigDecimal decimal) {
            return decimal;
        }
        return BigDecimal.valueOf(Double.parseDouble(val.toString()));
    }
}
