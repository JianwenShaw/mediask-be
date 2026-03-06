package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.DoctorTimeOffDO;
import me.jianwen.mediask.dal.mapper.DoctorTimeOffMapper;
import me.jianwen.mediask.schedule.domain.rule.DoctorTimeOff;
import me.jianwen.mediask.schedule.domain.repository.DoctorTimeOffRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DoctorTimeOffRepositoryImpl implements DoctorTimeOffRepository {

    private final DoctorTimeOffMapper mapper;

    @Override
    public List<DoctorTimeOff> listByDoctorIdsAndDateRange(List<Long> doctorIds, LocalDate startDate, LocalDate endDate) {
        if (doctorIds == null || doctorIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<DoctorTimeOffDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DoctorTimeOffDO::getDoctorId, doctorIds)
                .eq(DoctorTimeOffDO::getStatus, 1)
                .le(DoctorTimeOffDO::getStartDate, endDate)
                .ge(DoctorTimeOffDO::getEndDate, startDate)
                .orderByAsc(DoctorTimeOffDO::getDoctorId, DoctorTimeOffDO::getStartDate);
        return mapper.selectList(wrapper).stream()
                .map(data -> new DoctorTimeOff(
                        data.getDoctorId(),
                        data.getStartDate(),
                        data.getEndDate(),
                        data.getPeriodCode()
                ))
                .toList();
    }
}
