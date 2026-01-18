package me.jianwen.mediask.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班定时任务处理器
 *
 * 处理排班过期等定时任务
 *
 * @author jianwen
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduleScheduler {

    private final DoctorScheduleRepository scheduleRepository;

    /**
     * 标记过期排班
     * 每日凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void markExpiredSchedules() {
        log.info("开始标记过期排班任务");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<DoctorSchedule> schedules = scheduleRepository.findExpiredSchedules(yesterday);

        int markedCount = 0;
        for (DoctorSchedule schedule : schedules) {
            try {
                schedule.markAsExpired();
                scheduleRepository.save(schedule);
                log.info("已标记过期排班: scheduleId={}, doctorId={}, date={}",
                        schedule.getId().getValue(),
                        schedule.getDoctorId().getValue(),
                        schedule.getScheduleDate());
                markedCount++;
            } catch (Exception e) {
                log.error("排班过期标记失败: scheduleId={}", schedule.getId().getValue(), e);
            }
        }

        log.info("排班过期标记任务完成: 共标记 {} 条", markedCount);
    }

    /**
     * 清理历史数据（可选）
     * 每月1日凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void cleanupOldSchedules() {
        log.info("开始清理历史排班数据任务");

        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);

        List<DoctorSchedule> allSchedules = scheduleRepository.findExpiredSchedules(threeMonthsAgo);

        int deletedCount = 0;
        for (DoctorSchedule schedule : allSchedules) {
            if (schedule.getStatus() == ScheduleStatus.EXPIRED) {
                try {
                    scheduleRepository.remove(schedule.getId());
                    log.info("已删除过期排班: scheduleId={}", schedule.getId().getValue());
                    deletedCount++;
                } catch (Exception e) {
                    log.error("排班删除失败: scheduleId={}", schedule.getId().getValue(), e);
                }
            }
        }

        log.info("历史排班清理任务完成: 共删除 {} 条", deletedCount);
    }
}
