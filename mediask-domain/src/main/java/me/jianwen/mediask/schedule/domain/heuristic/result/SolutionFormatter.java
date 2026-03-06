package me.jianwen.mediask.schedule.domain.heuristic.result;

import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.heuristic.problem.Doctor;
import me.jianwen.mediask.schedule.domain.heuristic.context.SolverContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 解决方案格式化器
 *
 * <p>将排班解决方案转换为各种输出格式：
 * <ul>
 *   <li>JSON格式</li>
 *   <li>可视化表格</li>
 *   <li>统计报告</li>
 * </ul>
 *
 * @author MediAsk
 */
@Slf4j
public class SolutionFormatter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 格式化为JSON字符串
     */
    public static String toJson(ScheduleSolution solution) {
        if (!solution.isSuccess()) {
            return String.format("{\"error\": \"%s\"}", solution.getErrorMessage());
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"success\": true,\n");
        json.append("  \"solver\": \"").append(solution.getSolverName()).append("\",\n");
        json.append("  \"score\": ").append(String.format("%.2f", solution.getScore())).append(",\n");
        json.append("  \"assignments\": {\n");

        var assignments = solution.getContext().getAssignments();
        int count = 0;
        for (Map.Entry<String, Long> entry : assignments.entrySet()) {
            String[] parts = entry.getKey().split("_");
            if (parts.length < 2) continue;

            String date = parts[0];
            String period = parts[1];
            Long doctorId = entry.getValue();

            json.append("    \"").append(date).append("_").append(period).append("\": ");
            json.append(doctorId);
            if (++count < assignments.size()) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  },\n");
        json.append("  \"summary\": {\n");
        json.append("    \"totalAssignments\": ").append(solution.getAssignmentCount()).append(",\n");
        json.append("    \"hardConstraintsMet\": ").append(solution.isHardConstraintsSatisfied()).append(",\n");
        json.append("    \"softSatisfaction\": ").append(String.format("%.2f%%",
                solution.getSoftConstraintsSatisfaction() * 100)).append(",\n");
        json.append("    \"generationTimeMs\": ").append(solution.getGenerationTimeMs()).append("\n");
        json.append("  }\n");
        json.append("}");

        return json.toString();
    }

    /**
     * 格式化为可视化表格字符串
     */
    public static String toTable(ScheduleSolution solution) {
        if (!solution.isSuccess()) {
            return "Error: " + solution.getErrorMessage();
        }

        StringBuilder sb = new StringBuilder();
        var context = solution.getContext();
        var problem = solution.getProblem();
        var dateRange = problem.getDateRange();
        var timeConfig = problem.getTimeConfig();

        sb.append("\n========== 排班结果 ==========\n");
        sb.append(String.format("求解器: %s | 评分: %.2f | 排班数: %d\n",
                solution.getSolverName(),
                solution.getScore(),
                solution.getAssignmentCount()));
        sb.append("==============================\n");

        // 获取所有日期
        List<LocalDate> dates = dateRange.getAllDates();

        // 按日期分组显示
        for (LocalDate date : dates) {
            boolean isWeekend = dateRange.isWeekend(date);
            String dateStr = date.format(DATE_FORMAT);
            String dayOfWeek = date.getDayOfWeek().toString();

            sb.append(String.format("\n[%s %s] %s\n",
                    dateStr,
                    dayOfWeek,
                    isWeekend ? "(周末)" : ""));

            for (String period : timeConfig.getEnabledPeriods()) {
                Long doctorId = context.getAssignedDoctor(date, period);
                String doctorName = "未排班";

                if (doctorId != null) {
                    Optional<Doctor> doctor = problem.getDoctorSet().getById(doctorId);
                    if (doctor.isPresent()) {
                        doctorName = doctor.get().getName() +
                                (doctor.get().isExpert() ? "(专家)" : "");
                    }
                }

                String periodLabel = switch (period) {
                    case "MORNING" -> "上午";
                    case "AFTERNOON" -> "下午";
                    case "EVENING" -> "晚上";
                    default -> period;
                };

                sb.append(String.format("  %s: %s\n", periodLabel, doctorName));
            }
        }

        sb.append("\n==============================\n");
        sb.append(String.format("硬约束满足: %s | 软约束满意度: %.2f%%\n",
                solution.isHardConstraintsSatisfied() ? "是" : "否",
                solution.getSoftConstraintsSatisfaction() * 100));
        sb.append(String.format("生成耗时: %d ms\n", solution.getGenerationTimeMs()));

        return sb.toString();
    }

    /**
     * 格式化为统计报告
     */
    public static String toReport(ScheduleSolution solution) {
        if (!solution.isSuccess()) {
            return "Error: " + solution.getErrorMessage();
        }

        StringBuilder sb = new StringBuilder();
        var context = solution.getContext();
        var problem = solution.getProblem();
        var doctorSet = problem.getDoctorSet();

        sb.append("\n========== 排班统计报告 ==========\n\n");

        // 医生工作量统计
        sb.append("【医生工作量统计】\n");
        var workloads = context.getTotalWorkload();
        workloads.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    Long doctorId = entry.getKey();
                    int count = entry.getValue();
                    Optional<Doctor> doctor = doctorSet.getById(doctorId);
                    String name = doctor.map(Doctor::getName).orElse("未知");
                    sb.append(String.format("  %s (%s): %d 次\n",
                            name,
                            doctor.map(d -> d.isExpert() ? "专家" : "普通").orElse(""),
                            count));
                });

        // 专家覆盖统计
        sb.append("\n【专家覆盖统计】\n");
        long expertCount = workloads.entrySet().stream()
                .filter(e -> {
                    Optional<Doctor> d = doctorSet.getById(e.getKey());
                    return d.map(Doctor::isExpert).orElse(false);
                })
                .mapToInt(Map.Entry::getValue)
                .sum();
        long total = solution.getAssignmentCount();
        double rate = total > 0 ? (double) expertCount / total * 100 : 0;
        sb.append(String.format("  专家排班: %d / %d (%.1f%%)\n", expertCount, total, rate));

        // 约束满足情况
        sb.append("\n【约束满足情况】\n");
        for (var result : solution.getConstraintResults()) {
            String status = result.satisfied() ? "✓" : "✗";
            sb.append(String.format("  %s %s: %s\n",
                    status,
                    result.message(),
                    result.satisfied() ? "" : String.format("(惩罚=%.2f)", result.penalty())));
        }

        sb.append("\n==================================\n");

        return sb.toString();
    }
}
