package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.common.dto.ai.AiDepartmentMetricsDTO;
import me.jianwen.mediask.common.dto.ai.AiOverviewMetricsDTO;
import me.jianwen.mediask.schedule.domain.entity.AiFeedbackReview;

import java.time.LocalDate;
import java.util.List;

public interface AiMetricsRepository {

    void saveReview(AiFeedbackReview review);

    AiOverviewMetricsDTO getOverviewMetrics(LocalDate date);

    List<AiDepartmentMetricsDTO> listDepartmentMetrics(LocalDate date);
}
