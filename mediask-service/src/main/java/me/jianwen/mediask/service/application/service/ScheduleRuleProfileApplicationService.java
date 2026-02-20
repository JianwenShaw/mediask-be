package me.jianwen.mediask.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.dto.schedule.ScheduleRuleProfileDTO;
import me.jianwen.mediask.common.dto.schedule.ScheduleRuleProfilePublishResultDTO;
import me.jianwen.mediask.common.dto.schedule.ScheduleRuleProfileVersionDTO;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.util.JsonUtil;
import me.jianwen.mediask.infra.schedule.engine.dsl.ConstraintDslVersionManager;
import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleRuleProfile;
import me.jianwen.mediask.schedule.domain.repository.ScheduleRuleProfileRepository;
import me.jianwen.mediask.service.application.command.CreateScheduleRuleProfileCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * 排班规则配置应用服务。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleRuleProfileApplicationService {

    private static final String DEFAULT_NAMESPACE = "DEFAULT";

    private final ScheduleRuleProfileRepository scheduleRuleProfileRepository;
    private final ConstraintDslVersionManager constraintDslVersionManager;

    @Transactional(rollbackFor = Exception.class)
    public ScheduleRuleProfileDTO createDraft(CreateScheduleRuleProfileCommand command) {
        validateCreateCommand(command);
        String profileCode = normalizeProfileCode(command.getProfileCode());
        int nextVersion = scheduleRuleProfileRepository.findLatestVersion(command.getDepartmentId(), profileCode) + 1;
        Long profileId = scheduleRuleProfileRepository.save(new ScheduleRuleProfile(
                null,
                command.getDepartmentId(),
                profileCode,
                command.getProfileName(),
                nextVersion,
                "DRAFT",
                command.getConstraintDslJson(),
                command.getDescription(),
                command.getOperatorId(),
                null,
                null
        ));
        log.info("创建规则配置草稿成功: departmentId={}, profileCode={}, version={}",
                command.getDepartmentId(), profileCode, nextVersion);
        return getProfile(profileId);
    }

    public ScheduleRuleProfileDTO getProfile(Long profileId) {
        ScheduleRuleProfile profile = scheduleRuleProfileRepository.findById(profileId)
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "规则配置不存在"));
        return toDTO(profile);
    }

    public List<ScheduleRuleProfileVersionDTO> listVersions(Long departmentId, String profileCode) {
        if (departmentId == null || profileCode == null || profileCode.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "departmentId/profileCode 不能为空");
        }
        return scheduleRuleProfileRepository.listByProfileCode(departmentId, normalizeProfileCode(profileCode)).stream()
                .map(this::toVersionDTO)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ScheduleRuleProfilePublishResultDTO publish(Long profileId) {
        return activateProfile(profileId, "发布成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public ScheduleRuleProfilePublishResultDTO rollback(Long profileId) {
        return activateProfile(profileId, "回滚发布成功");
    }

    private ScheduleRuleProfilePublishResultDTO activateProfile(Long profileId, String actionMessage) {
        ScheduleRuleProfile profile = scheduleRuleProfileRepository.findById(profileId)
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "规则配置不存在"));
        scheduleRuleProfileRepository.archivePublished(profile.departmentId(), profile.profileCode());
        scheduleRuleProfileRepository.updateStatus(profileId, "PUBLISHED");
        long dslVersion = constraintDslVersionManager.bumpVersion(DEFAULT_NAMESPACE, profile.departmentId());
        return new ScheduleRuleProfilePublishResultDTO(
                profile.profileId(),
                profile.profileCode(),
                profile.versionNo(),
                "PUBLISHED",
                dslVersion,
                actionMessage
        );
    }

    private void validateCreateCommand(CreateScheduleRuleProfileCommand command) {
        if (command.getDepartmentId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "departmentId 不能为空");
        }
        if (command.getProfileCode() == null || command.getProfileCode().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "profileCode 不能为空");
        }
        if (command.getProfileName() == null || command.getProfileName().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "profileName 不能为空");
        }
        if (command.getConstraintDslJson() == null || command.getConstraintDslJson().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "constraintDslJson 不能为空");
        }
        try {
            var node = JsonUtil.getObjectMapper().readTree(command.getConstraintDslJson());
            if (!node.isObject()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "constraintDslJson 必须是 JSON 对象");
            }
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.PARAM_INVALID, "constraintDslJson 非法: " + exception.getMessage());
        }
    }

    private String normalizeProfileCode(String profileCode) {
        return profileCode.trim().toUpperCase(Locale.ROOT);
    }

    private ScheduleRuleProfileDTO toDTO(ScheduleRuleProfile profile) {
        return new ScheduleRuleProfileDTO(
                profile.profileId(),
                profile.departmentId(),
                profile.profileCode(),
                profile.profileName(),
                profile.versionNo(),
                profile.profileStatus(),
                profile.constraintDslJson(),
                profile.description()
        );
    }

    private ScheduleRuleProfileVersionDTO toVersionDTO(ScheduleRuleProfile profile) {
        return new ScheduleRuleProfileVersionDTO(
                profile.profileId(),
                profile.departmentId(),
                profile.profileCode(),
                profile.profileName(),
                profile.versionNo(),
                profile.profileStatus(),
                profile.updatedAt()
        );
    }
}
