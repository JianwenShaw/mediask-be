package me.jianwen.mediask.schedule.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.AppointmentSlotDO;
import me.jianwen.mediask.dal.mapper.AppointmentSlotMapper;
import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.repository.AppointmentSlotRepository;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.infrastructure.converter.AppointmentSlotConverter;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 号源时段仓储实现（基础设施层）
 *
 * @author jianwen
 */
@Repository
@RequiredArgsConstructor
public class AppointmentSlotRepositoryImpl implements AppointmentSlotRepository {

    private final AppointmentSlotMapper slotMapper;
    private final AppointmentSlotConverter slotConverter;

    @Override
    public void save(AppointmentSlot slot) {
        AppointmentSlotDO dataObject = slotConverter.toDataObject(slot);
        if (dataObject == null) {
            return;
        }

        if (dataObject.getId() == null) {
            slotMapper.insert(dataObject);
            slot.setId(dataObject.getId());
        } else {
            slotMapper.updateById(dataObject);
        }
    }

    @Override
    public void saveAll(List<AppointmentSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        slots.forEach(this::save);
    }

    @Override
    public Optional<AppointmentSlot> findById(Long id) {
        AppointmentSlotDO dataObject = slotMapper.selectById(id);
        if (dataObject == null) {
            return Optional.empty();
        }
        return Optional.of(slotConverter.toDomain(dataObject));
    }

    @Override
    public List<AppointmentSlot> findBySchedule(ScheduleId scheduleId) {
        LambdaQueryWrapper<AppointmentSlotDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentSlotDO::getScheduleId, scheduleId.getValue())
                .orderByAsc(AppointmentSlotDO::getSlotTime);

        return slotMapper.selectList(wrapper).stream()
                .map(slotConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentSlot> findAvailableBySchedule(ScheduleId scheduleId) {
        LambdaQueryWrapper<AppointmentSlotDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentSlotDO::getScheduleId, scheduleId.getValue())
                .eq(AppointmentSlotDO::getIsOccupied, 0)
                .orderByAsc(AppointmentSlotDO::getSlotTime);

        return slotMapper.selectList(wrapper).stream()
                .map(slotConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AppointmentSlot> findByScheduleAndTime(ScheduleId scheduleId, LocalTime startTime) {
        LambdaQueryWrapper<AppointmentSlotDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentSlotDO::getScheduleId, scheduleId.getValue())
                .eq(AppointmentSlotDO::getSlotTime, startTime);

        AppointmentSlotDO dataObject = slotMapper.selectOne(wrapper);
        if (dataObject == null) {
            return Optional.empty();
        }
        return Optional.of(slotConverter.toDomain(dataObject));
    }

    @Override
    public long countAvailableBySchedule(ScheduleId scheduleId) {
        LambdaQueryWrapper<AppointmentSlotDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentSlotDO::getScheduleId, scheduleId.getValue())
                .eq(AppointmentSlotDO::getIsOccupied, 0);
        return slotMapper.selectCount(wrapper);
    }

    @Override
    public void deleteBySchedule(ScheduleId scheduleId) {
        LambdaQueryWrapper<AppointmentSlotDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentSlotDO::getScheduleId, scheduleId.getValue());
        slotMapper.delete(wrapper);
    }
}


