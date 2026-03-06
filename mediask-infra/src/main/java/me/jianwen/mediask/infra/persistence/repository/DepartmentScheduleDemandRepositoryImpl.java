package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.DepartmentScheduleDemandDO;
import me.jianwen.mediask.dal.mapper.DepartmentScheduleDemandMapper;
import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleDemand;
import me.jianwen.mediask.schedule.domain.repository.DepartmentScheduleDemandRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DepartmentScheduleDemandRepositoryImpl implements DepartmentScheduleDemandRepository {

    private final DepartmentScheduleDemandMapper mapper;

    @Override
    public List<DepartmentScheduleDemand> listByDepartmentAndDateRange(Long departmentId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DepartmentScheduleDemandDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepartmentScheduleDemandDO::getDepartmentId, departmentId)
                .eq(DepartmentScheduleDemandDO::getStatus, 1)
                .ge(DepartmentScheduleDemandDO::getDemandDate, startDate)
                .le(DepartmentScheduleDemandDO::getDemandDate, endDate)
                .orderByAsc(DepartmentScheduleDemandDO::getDemandDate, DepartmentScheduleDemandDO::getPeriodCode);
        return mapper.selectList(wrapper).stream()
                .map(data -> new DepartmentScheduleDemand(
                        data.getDepartmentId(),
                        data.getDemandDate(),
                        data.getPeriodCode(),
                        data.getRequiredDoctors(),
                        data.getMinSeniorDoctors()
                ))
                .toList();
    }
}
