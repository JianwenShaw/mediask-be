package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.SchedulePlanConstraintSnapshotDO;
import me.jianwen.mediask.dal.entity.SchedulePlanDO;
import me.jianwen.mediask.dal.entity.SchedulePlanItemDO;
import me.jianwen.mediask.dal.mapper.SchedulePlanConstraintSnapshotMapper;
import me.jianwen.mediask.dal.mapper.SchedulePlanItemMapper;
import me.jianwen.mediask.dal.mapper.SchedulePlanMapper;
import me.jianwen.mediask.schedule.domain.optimization.model.SchedulePlan;
import me.jianwen.mediask.schedule.domain.optimization.model.SchedulePlanConstraintSnapshot;
import me.jianwen.mediask.schedule.domain.optimization.model.SchedulePlanItem;
import me.jianwen.mediask.schedule.domain.repository.SchedulePlanRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SchedulePlanRepositoryImpl implements SchedulePlanRepository {

    private final SchedulePlanMapper schedulePlanMapper;
    private final SchedulePlanItemMapper schedulePlanItemMapper;
    private final SchedulePlanConstraintSnapshotMapper snapshotMapper;

    @Override
    public int findLatestVersion(String planCode) {
        LambdaQueryWrapper<SchedulePlanDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchedulePlanDO::getPlanCode, planCode)
                .orderByDesc(SchedulePlanDO::getVersionNo)
                .last("LIMIT 1");
        SchedulePlanDO latest = schedulePlanMapper.selectOne(wrapper);
        return latest == null || latest.getVersionNo() == null ? 0 : latest.getVersionNo();
    }

    @Override
    public Long savePlan(SchedulePlan plan) {
        SchedulePlanDO data = new SchedulePlanDO();
        data.setPlanCode(plan.planCode());
        data.setDepartmentId(plan.departmentId());
        data.setStartDate(plan.startDate());
        data.setEndDate(plan.endDate());
        data.setVersionNo(plan.versionNo());
        data.setPlanStatus(plan.planStatus());
        data.setSolverStrategy(plan.solverStrategy());
        data.setGeneratedBy(plan.generatedBy());
        data.setTotalScore(BigDecimal.valueOf(plan.totalScore()));
        data.setHardViolationCount(plan.hardViolationCount());
        data.setWarningsJson(plan.warningsJson());
        schedulePlanMapper.insert(data);
        return data.getId();
    }

    @Override
    public void savePlanItems(Long planId, List<SchedulePlanItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (SchedulePlanItem item : items) {
            SchedulePlanItemDO data = new SchedulePlanItemDO();
            data.setPlanId(planId);
            data.setScheduleDate(item.scheduleDate());
            data.setPeriodCode(item.periodCode());
            data.setDoctorId(item.doctorId());
            data.setIsSenior(item.senior() ? 1 : 0);
            data.setReasonJson(item.reasonJson());
            data.setPenaltyJson(item.penaltyJson());
            data.setScoreDelta(item.scoreDelta() == null ? null : BigDecimal.valueOf(item.scoreDelta()));
            schedulePlanItemMapper.insert(data);
        }
    }

    @Override
    public void saveConstraintSnapshots(Long planId, List<SchedulePlanConstraintSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (SchedulePlanConstraintSnapshot snapshot : snapshots) {
            SchedulePlanConstraintSnapshotDO data = new SchedulePlanConstraintSnapshotDO();
            data.setPlanId(planId);
            data.setSnapshotType(snapshot.snapshotType());
            data.setSnapshotJson(snapshot.snapshotJson());
            snapshotMapper.insert(data);
        }
    }

    @Override
    public Optional<SchedulePlan> findById(Long planId) {
        SchedulePlanDO data = schedulePlanMapper.selectById(planId);
        if (data == null) {
            return Optional.empty();
        }
        return Optional.of(toModel(data));
    }

    @Override
    public List<SchedulePlan> listByPlanCode(String planCode) {
        LambdaQueryWrapper<SchedulePlanDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchedulePlanDO::getPlanCode, planCode)
                .orderByDesc(SchedulePlanDO::getVersionNo);
        return schedulePlanMapper.selectList(wrapper).stream().map(this::toModel).toList();
    }

    @Override
    public List<SchedulePlanItem> listPlanItems(Long planId) {
        LambdaQueryWrapper<SchedulePlanItemDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchedulePlanItemDO::getPlanId, planId)
                .orderByAsc(SchedulePlanItemDO::getScheduleDate, SchedulePlanItemDO::getPeriodCode, SchedulePlanItemDO::getDoctorId);
        return schedulePlanItemMapper.selectList(wrapper).stream()
                .map(data -> new SchedulePlanItem(
                        data.getScheduleDate(),
                        data.getPeriodCode(),
                        data.getDoctorId(),
                        data.getIsSenior() != null && data.getIsSenior() == 1,
                        data.getReasonJson(),
                        data.getPenaltyJson(),
                        data.getScoreDelta() == null ? null : data.getScoreDelta().doubleValue()
                ))
                .toList();
    }

    @Override
    public void updatePlanStatus(Long planId, String status) {
        SchedulePlanDO data = new SchedulePlanDO();
        data.setId(planId);
        data.setPlanStatus(status);
        if (schedulePlanMapper.updateById(data) <= 0) {
            throw new BizException(ErrorCode.DATABASE_ERROR, "更新排班方案状态失败");
        }
    }

    @Override
    public void archivePublishedPlans(String planCode) {
        LambdaQueryWrapper<SchedulePlanDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchedulePlanDO::getPlanCode, planCode)
                .eq(SchedulePlanDO::getPlanStatus, "PUBLISHED");
        List<SchedulePlanDO> published = schedulePlanMapper.selectList(wrapper);
        for (SchedulePlanDO item : published) {
            SchedulePlanDO update = new SchedulePlanDO();
            update.setId(item.getId());
            update.setPlanStatus("ARCHIVED");
            schedulePlanMapper.updateById(update);
        }
    }

    private SchedulePlan toModel(SchedulePlanDO data) {
        return new SchedulePlan(
                data.getId(),
                data.getPlanCode(),
                data.getDepartmentId(),
                data.getStartDate(),
                data.getEndDate(),
                data.getVersionNo(),
                data.getPlanStatus(),
                data.getSolverStrategy(),
                data.getGeneratedBy(),
                data.getTotalScore() == null ? 0D : data.getTotalScore().doubleValue(),
                data.getHardViolationCount() == null ? 0 : data.getHardViolationCount(),
                data.getWarningsJson()
        );
    }
}
