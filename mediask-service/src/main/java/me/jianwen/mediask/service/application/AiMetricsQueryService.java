package me.jianwen.mediask.service.application;

import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.schedule.domain.readmodel.AiDepartmentMetrics;
import me.jianwen.mediask.schedule.domain.readmodel.AiOverviewMetrics;
import me.jianwen.mediask.schedule.domain.repository.AiMetricsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 指标查询服务
 */
@Service
@RequiredArgsConstructor
public class AiMetricsQueryService {

    private final AiMetricsRepository aiMetricsRepository;

    public AiOverviewMetrics getOverviewMetrics(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return aiMetricsRepository.getOverviewMetrics(targetDate);
    }

    public List<AiDepartmentMetrics> listDepartmentMetrics(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return aiMetricsRepository.listDepartmentMetrics(targetDate);
    }
}
