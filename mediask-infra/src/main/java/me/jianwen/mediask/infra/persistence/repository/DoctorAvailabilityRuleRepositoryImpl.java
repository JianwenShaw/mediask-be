package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.DoctorAvailabilityRuleDO;
import me.jianwen.mediask.dal.mapper.DoctorAvailabilityRuleMapper;
import me.jianwen.mediask.schedule.domain.optimization.model.DoctorAvailabilityRule;
import me.jianwen.mediask.schedule.domain.repository.DoctorAvailabilityRuleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DoctorAvailabilityRuleRepositoryImpl implements DoctorAvailabilityRuleRepository {

    private final DoctorAvailabilityRuleMapper mapper;

    @Override
    public List<DoctorAvailabilityRule> listByDoctorIds(List<Long> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<DoctorAvailabilityRuleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DoctorAvailabilityRuleDO::getDoctorId, doctorIds)
                .eq(DoctorAvailabilityRuleDO::getStatus, 1)
                .orderByAsc(DoctorAvailabilityRuleDO::getDoctorId,
                        DoctorAvailabilityRuleDO::getWeekday,
                        DoctorAvailabilityRuleDO::getPeriodCode);
        return mapper.selectList(wrapper).stream()
                .map(this::toModel)
                .toList();
    }

    private DoctorAvailabilityRule toModel(DoctorAvailabilityRuleDO data) {
        return new DoctorAvailabilityRule(
                data.getDoctorId(),
                data.getWeekday(),
                data.getPeriodCode(),
                data.getIsAvailable() != null && data.getIsAvailable() == 1,
                data.getPriority()
        );
    }
}
