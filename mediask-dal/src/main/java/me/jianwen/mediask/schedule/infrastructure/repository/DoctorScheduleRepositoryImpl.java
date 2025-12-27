package me.jianwen.mediask.schedule.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.DoctorScheduleDO;
import me.jianwen.mediask.dal.mapper.DoctorScheduleMapper;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleStatus;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
import me.jianwen.mediask.schedule.infrastructure.converter.ScheduleConverter;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 排班仓储实现（基础设施层）
 *
 * @author jianwen
 */
@Repository
@RequiredArgsConstructor
public class DoctorScheduleRepositoryImpl implements DoctorScheduleRepository {

    private final DoctorScheduleMapper scheduleMapper;
    private final ScheduleConverter scheduleConverter;

    @Override
    public void save(DoctorSchedule schedule) {
        DoctorScheduleDO dataObject = scheduleConverter.toDataObject(schedule);

        if (dataObject.getId() == null) {
            scheduleMapper.insert(dataObject);
            // 回填ID
            schedule.setId(ScheduleId.of(dataObject.getId()));
        } else {
            scheduleMapper.updateById(dataObject);
        }
    }

    @Override
    public void saveAll(List<DoctorSchedule> schedules) {
        schedules.forEach(this::save);
    }

    @Override
    public Optional<DoctorSchedule> findById(ScheduleId scheduleId) {
        DoctorScheduleDO dataObject = scheduleMapper.selectById(scheduleId.getValue());
        if (dataObject == null) {
            return Optional.empty();
        }
        return Optional.of(scheduleConverter.toDomain(dataObject));
    }

    @Override
    public Optional<DoctorSchedule> findByDoctorAndDateAndPeriod(
            DoctorId doctorId,
            LocalDate scheduleDate,
            TimePeriod timePeriod) {

        LambdaQueryWrapper<DoctorScheduleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorScheduleDO::getDoctorId, doctorId.getValue())
                .eq(DoctorScheduleDO::getScheduleDate, scheduleDate)
                .eq(DoctorScheduleDO::getTimePeriod, timePeriod.getCode());

        DoctorScheduleDO dataObject = scheduleMapper.selectOne(wrapper);
        if (dataObject == null) {
            return Optional.empty();
        }
        return Optional.of(scheduleConverter.toDomain(dataObject));
    }

    @Override
    public List<DoctorSchedule> findByDoctorAndDate(DoctorId doctorId, LocalDate scheduleDate) {
        LambdaQueryWrapper<DoctorScheduleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorScheduleDO::getDoctorId, doctorId.getValue())
                .eq(DoctorScheduleDO::getScheduleDate, scheduleDate)
                .orderByAsc(DoctorScheduleDO::getTimePeriod);

        return scheduleMapper.selectList(wrapper).stream()
                .map(scheduleConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoctorSchedule> findByDoctorAndDateRange(
            DoctorId doctorId,
            LocalDate startDate,
            LocalDate endDate) {

        LambdaQueryWrapper<DoctorScheduleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorScheduleDO::getDoctorId, doctorId.getValue())
                .ge(DoctorScheduleDO::getScheduleDate, startDate)
                .le(DoctorScheduleDO::getScheduleDate, endDate)
                .orderByAsc(DoctorScheduleDO::getScheduleDate, DoctorScheduleDO::getTimePeriod);

        return scheduleMapper.selectList(wrapper).stream()
                .map(scheduleConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoctorSchedule> findOpenSchedulesByDateAndPeriod(
            LocalDate scheduleDate,
            TimePeriod timePeriod) {

        LambdaQueryWrapper<DoctorScheduleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorScheduleDO::getScheduleDate, scheduleDate)
                .eq(DoctorScheduleDO::getTimePeriod, timePeriod.getCode())
                .eq(DoctorScheduleDO::getStatus, ScheduleStatus.OPEN.getCode());

        return scheduleMapper.selectList(wrapper).stream()
                .map(scheduleConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(DoctorId doctorId, LocalDate scheduleDate, TimePeriod timePeriod) {
        LambdaQueryWrapper<DoctorScheduleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorScheduleDO::getDoctorId, doctorId.getValue())
                .eq(DoctorScheduleDO::getScheduleDate, scheduleDate)
                .eq(DoctorScheduleDO::getTimePeriod, timePeriod.getCode());

        return scheduleMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<DoctorSchedule> findExpiredSchedules(LocalDate beforeDate) {
        LambdaQueryWrapper<DoctorScheduleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(DoctorScheduleDO::getScheduleDate, beforeDate)
                .ne(DoctorScheduleDO::getStatus, ScheduleStatus.EXPIRED.getCode());

        return scheduleMapper.selectList(wrapper).stream()
                .map(scheduleConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void remove(ScheduleId scheduleId) {
        scheduleMapper.deleteById(scheduleId.getValue());
    }
}
