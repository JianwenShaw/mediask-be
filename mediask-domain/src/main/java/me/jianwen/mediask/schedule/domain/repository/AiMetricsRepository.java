package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.entity.AiFeedbackReview;
import me.jianwen.mediask.schedule.domain.readmodel.AiDepartmentMetrics;
import me.jianwen.mediask.schedule.domain.readmodel.AiOverviewMetrics;

import java.time.LocalDate;
import java.util.List;

public interface AiMetricsRepository {

    void saveReview(AiFeedbackReview review);

    AiOverviewMetrics getOverviewMetrics(LocalDate date);

    List<AiDepartmentMetrics> listDepartmentMetrics(LocalDate date);
}
