package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.dal.entity.ScheduleRuleProfileDO;
import me.jianwen.mediask.dal.mapper.ScheduleRuleProfileMapper;
import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleRuleProfile;
import me.jianwen.mediask.schedule.domain.repository.ScheduleRuleProfileRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 排班规则配置仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class ScheduleRuleProfileRepositoryImpl implements ScheduleRuleProfileRepository {

    private final ScheduleRuleProfileMapper scheduleRuleProfileMapper;

    @Override
    public int findLatestVersion(Long departmentId, String profileCode) {
        LambdaQueryWrapper<ScheduleRuleProfileDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleRuleProfileDO::getDepartmentId, departmentId)
                .eq(ScheduleRuleProfileDO::getProfileCode, profileCode)
                .orderByDesc(ScheduleRuleProfileDO::getVersionNo)
                .last("LIMIT 1");
        ScheduleRuleProfileDO latest = scheduleRuleProfileMapper.selectOne(wrapper);
        return latest == null || latest.getVersionNo() == null ? 0 : latest.getVersionNo();
    }

    @Override
    public Long save(ScheduleRuleProfile profile) {
        ScheduleRuleProfileDO data = toDO(profile);
        scheduleRuleProfileMapper.insert(data);
        return data.getId();
    }

    @Override
    public void updateStatus(Long profileId, String status) {
        ScheduleRuleProfileDO update = new ScheduleRuleProfileDO();
        update.setId(profileId);
        update.setProfileStatus(status);
        if (scheduleRuleProfileMapper.updateById(update) <= 0) {
            throw new BizException(ErrorCode.DATABASE_ERROR, "更新规则配置状态失败");
        }
    }

    @Override
    public Optional<ScheduleRuleProfile> findById(Long profileId) {
        ScheduleRuleProfileDO data = scheduleRuleProfileMapper.selectById(profileId);
        return data == null ? Optional.empty() : Optional.of(toModel(data));
    }

    @Override
    public List<ScheduleRuleProfile> listByProfileCode(Long departmentId, String profileCode) {
        LambdaQueryWrapper<ScheduleRuleProfileDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleRuleProfileDO::getDepartmentId, departmentId)
                .eq(ScheduleRuleProfileDO::getProfileCode, profileCode)
                .orderByDesc(ScheduleRuleProfileDO::getVersionNo);
        return scheduleRuleProfileMapper.selectList(wrapper).stream().map(this::toModel).toList();
    }

    @Override
    public Optional<ScheduleRuleProfile> findPublishedByCode(Long departmentId, String profileCode) {
        LambdaQueryWrapper<ScheduleRuleProfileDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleRuleProfileDO::getDepartmentId, departmentId)
                .eq(ScheduleRuleProfileDO::getProfileCode, profileCode)
                .eq(ScheduleRuleProfileDO::getProfileStatus, "PUBLISHED")
                .orderByDesc(ScheduleRuleProfileDO::getVersionNo)
                .last("LIMIT 1");
        ScheduleRuleProfileDO data = scheduleRuleProfileMapper.selectOne(wrapper);
        return data == null ? Optional.empty() : Optional.of(toModel(data));
    }

    @Override
    public void archivePublished(Long departmentId, String profileCode) {
        LambdaQueryWrapper<ScheduleRuleProfileDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleRuleProfileDO::getDepartmentId, departmentId)
                .eq(ScheduleRuleProfileDO::getProfileCode, profileCode)
                .eq(ScheduleRuleProfileDO::getProfileStatus, "PUBLISHED");
        List<ScheduleRuleProfileDO> published = scheduleRuleProfileMapper.selectList(wrapper);
        for (ScheduleRuleProfileDO item : published) {
            ScheduleRuleProfileDO update = new ScheduleRuleProfileDO();
            update.setId(item.getId());
            update.setProfileStatus("ARCHIVED");
            scheduleRuleProfileMapper.updateById(update);
        }
    }

    private ScheduleRuleProfileDO toDO(ScheduleRuleProfile profile) {
        ScheduleRuleProfileDO data = new ScheduleRuleProfileDO();
        data.setId(profile.profileId());
        data.setDepartmentId(profile.departmentId());
        data.setProfileCode(profile.profileCode());
        data.setProfileName(profile.profileName());
        data.setVersionNo(profile.versionNo());
        data.setProfileStatus(profile.profileStatus());
        data.setConstraintDslJson(profile.constraintDslJson());
        data.setDescription(profile.description());
        data.setUpdatedBy(profile.updatedBy());
        return data;
    }

    private ScheduleRuleProfile toModel(ScheduleRuleProfileDO data) {
        return new ScheduleRuleProfile(
                data.getId(),
                data.getDepartmentId(),
                data.getProfileCode(),
                data.getProfileName(),
                data.getVersionNo(),
                data.getProfileStatus(),
                data.getConstraintDslJson(),
                data.getDescription(),
                data.getUpdatedBy(),
                data.getCreatedAt(),
                data.getUpdatedAt()
        );
    }
}
