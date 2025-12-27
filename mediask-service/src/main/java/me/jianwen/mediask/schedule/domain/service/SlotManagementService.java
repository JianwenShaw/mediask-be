package me.jianwen.mediask.schedule.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.repository.AppointmentSlotRepository;
import me.jianwen.mediask.schedule.domain.valueobject.TimeSlot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 号源时段管理领域服务
 * 负责时段的生成、分配和管理
 *
 * @author jianwen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SlotManagementService {

    private final AppointmentSlotRepository slotRepository;

    /**
     * 为排班生成时段
     * 根据排班的时段和每个号源的时长，自动生成时间片
     *
     * @param schedule 排班
     * @return 生成的时段列表
     */
    public List<AppointmentSlot> generateSlotsForSchedule(DoctorSchedule schedule) {
        log.info("为排班 {} 生成时段", schedule.getId());

        // 1. 生成时间片列表
        List<TimeSlot> timeSlots = schedule.generateTimeSlots();

        // 2. 限制时间片数量不超过总号源数
        List<TimeSlot> limitedSlots = timeSlots.stream()
                .limit(schedule.getCapacity().getTotalSlots())
                .collect(Collectors.toList());

        // 3. 创建号源时段实体
        List<AppointmentSlot> appointmentSlots = limitedSlots.stream()
                .map(timeSlot -> AppointmentSlot.createAvailable(schedule.getId(), timeSlot))
                .collect(Collectors.toList());

        log.info("为排班 {} 生成了 {} 个时段", schedule.getId(), appointmentSlots.size());

        return appointmentSlots;
    }

    /**
     * 批量保存时段
     */
    public void saveSlots(List<AppointmentSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        slotRepository.saveAll(slots);
        log.info("已保存 {} 个时段", slots.size());
    }

    /**
     * 占用时段
     */
    public void occupySlot(Long slotId, Long appointmentId) {
        AppointmentSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("时段不存在: " + slotId));

        slot.occupy(appointmentId);
        slotRepository.save(slot);

        log.info("时段 {} 已被预约 {} 占用", slotId, appointmentId);
    }

    /**
     * 释放时段
     */
    public void releaseSlot(Long slotId) {
        AppointmentSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("时段不存在: " + slotId));

        slot.release();
        slotRepository.save(slot);

        log.info("时段 {} 已释放", slotId);
    }
}
