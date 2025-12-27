package me.jianwen.mediask.schedule.domain.service;

import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;

import java.time.LocalDate;
import java.util.List;

/**
 * 自动排班策略接口
 * 使用策略模式，支持不同的排班策略
 *
 * @author jianwen
 */
public interface AutoScheduleStrategy {

    /**
     * 策略名称
     */
    String getStrategyName();

    /**
     * 生成排班
     *
     * @param doctorId  医生ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param context   排班上下文（包含规则、配置等）
     * @return 生成的排班列表
     */
    List<DoctorSchedule> generateSchedules(
            DoctorId doctorId,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleContext context);

    /**
     * 验证策略是否适用
     */
    boolean isApplicable(ScheduleContext context);
}
