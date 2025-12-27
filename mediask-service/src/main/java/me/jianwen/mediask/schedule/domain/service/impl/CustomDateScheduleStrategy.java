package me.jianwen.mediask.schedule.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.service.AutoScheduleStrategy;
import me.jianwen.mediask.schedule.domain.service.ScheduleContext;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自定义日期排班策略
 * 针对特定日期手动指定排班规则
 * 
 * 适用场景：
 * - 临时加班
 * - 特殊活动（义诊、专家会诊等）
 * - 节假日特殊排班
 *
 * @author jianwen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomDateScheduleStrategy implements AutoScheduleStrategy {

    @Override
    public String getStrategyName() {
        return "CUSTOM_DATE";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DoctorSchedule> generateSchedules(
            DoctorId doctorId,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleContext context) {

        log.info("使用自定义日期排班策略为医生 {} 生成排班", doctorId.getValue());

        List<DoctorSchedule> schedules = new ArrayList<>();

        // 从上下文获取自定义日期配置
        // 格式: Map<LocalDate, List<CustomPeriodConfig>>
        Map<LocalDate, List<CustomPeriodConfig>> dateConfigs = (Map<LocalDate, List<CustomPeriodConfig>>) context
                .getProperties().get("customDates");

        if (dateConfigs == null || dateConfigs.isEmpty()) {
            log.warn("未找到自定义日期配置");
            return schedules;
        }

        // 遍历配置的日期
        dateConfigs.forEach((date, periodConfigs) -> {
            // 检查日期是否在范围内
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {

                // 为每个时段生成排班
                for (CustomPeriodConfig config : periodConfigs) {
                    DoctorSchedule schedule = DoctorSchedule.create(
                            doctorId,
                            date,
                            config.getPeriod(),
                            config.getTotalSlots(),
                            config.getSlotDurationMinutes());
                    schedules.add(schedule);

                    log.debug("生成自定义排班: 日期={}, 时段={}, 号源数={}",
                            date, config.getPeriod().getDescription(), config.getTotalSlots());
                }
            }
        });

        log.info("自定义日期排班策略完成，共生成 {} 条排班", schedules.size());
        return schedules;
    }

    @Override
    public boolean isApplicable(ScheduleContext context) {
        return context.getProperties().containsKey("customDates");
    }

    /**
     * 自定义时段配置
     */
    @lombok.Data
    public static class CustomPeriodConfig {
        private TimePeriod period;
        private int totalSlots;
        private int slotDurationMinutes;
    }
}
