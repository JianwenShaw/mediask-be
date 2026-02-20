package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleRuleProfile;

import java.util.List;
import java.util.Optional;

/**
 * 排班规则配置仓储。
 */
public interface ScheduleRuleProfileRepository {

    int findLatestVersion(Long departmentId, String profileCode);

    Long save(ScheduleRuleProfile profile);

    void updateStatus(Long profileId, String status);

    Optional<ScheduleRuleProfile> findById(Long profileId);

    List<ScheduleRuleProfile> listByProfileCode(Long departmentId, String profileCode);

    Optional<ScheduleRuleProfile> findPublishedByCode(Long departmentId, String profileCode);

    void archivePublished(Long departmentId, String profileCode);
}
