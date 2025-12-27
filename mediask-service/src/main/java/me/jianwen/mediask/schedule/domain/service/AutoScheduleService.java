package me.jianwen.mediask.schedule.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自动排班领域服务
 * 协调排班策略，生成和管理自动排班
 *
 * @author jianwen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AutoScheduleService {

    private final List<AutoScheduleStrategy> strategies;
    private final DoctorScheduleRepository scheduleRepository;

    /**
     * 执行自动排班
     *
     * @param doctorId     医生ID
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @param context      排班上下文
     * @param strategyName 指定使用的策略名称（可选）
     * @return 生成的排班列表
     */
    public List<DoctorSchedule> autoSchedule(
            DoctorId doctorId,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleContext context,
            String strategyName) {

        log.info("开始自动排班: doctorId={}, dateRange={} to {}, strategy={}",
                doctorId.getValue(), startDate, endDate, strategyName);

        // 1. 选择策略
        AutoScheduleStrategy strategy = selectStrategy(strategyName, context);
        if (strategy == null) {
            throw new IllegalArgumentException("未找到可用的排班策略");
        }

        log.info("选择排班策略: {}", strategy.getStrategyName());

        // 2. 检查日期范围内是否已存在排班
        List<DoctorSchedule> existingSchedules = scheduleRepository
                .findByDoctorAndDateRange(doctorId, startDate, endDate);

        if (!existingSchedules.isEmpty()) {
            log.warn("日期范围内已存在 {} 条排班记录", existingSchedules.size());
            // 可以选择：抛出异常、跳过已存在的日期、或覆盖
            // 这里选择过滤掉已存在的日期
        }

        // 3. 执行策略生成排班
        List<DoctorSchedule> generatedSchedules = strategy.generateSchedules(
                doctorId, startDate, endDate, context);

        // 4. 过滤掉已存在的排班（避免重复）
        List<DoctorSchedule> newSchedules = filterExistingSchedules(
                generatedSchedules, existingSchedules);

        log.info("自动排班完成: 生成 {} 条新排班（跳过 {} 条已存在）",
                newSchedules.size(), generatedSchedules.size() - newSchedules.size());

        return newSchedules;
    }

    /**
     * 选择排班策略
     */
    private AutoScheduleStrategy selectStrategy(String strategyName, ScheduleContext context) {
        if (strategyName != null) {
            // 如果指定了策略名称，使用指定策略
            return strategies.stream()
                    .filter(s -> s.getStrategyName().equals(strategyName))
                    .findFirst()
                    .orElse(null);
        }

        // 否则，选择第一个适用的策略
        return strategies.stream()
                .filter(s -> s.isApplicable(context))
                .findFirst()
                .orElse(null);
    }

    /**
     * 过滤已存在的排班
     */
    private List<DoctorSchedule> filterExistingSchedules(
            List<DoctorSchedule> generated,
            List<DoctorSchedule> existing) {

        return generated.stream()
                .filter(g -> existing.stream().noneMatch(e -> e.getScheduleDate().equals(g.getScheduleDate()) &&
                        e.getTimePeriod().equals(g.getTimePeriod())))
                .collect(Collectors.toList());
    }

    /**
     * 批量保存排班
     */
    public void saveSchedules(List<DoctorSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return;
        }

        scheduleRepository.saveAll(schedules);
        log.info("已保存 {} 条排班记录", schedules.size());
    }
}
