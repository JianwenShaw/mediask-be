package me.jianwen.mediask.service.application.service;

import me.jianwen.mediask.common.dto.schedule.ScheduleRuleProfilePublishResultDTO;
import me.jianwen.mediask.infra.schedule.engine.dsl.ConstraintDslVersionManager;
import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleRuleProfile;
import me.jianwen.mediask.schedule.domain.repository.ScheduleRuleProfileRepository;
import me.jianwen.mediask.service.application.command.CreateScheduleRuleProfileCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleRuleProfileApplicationServiceTest {

    @Mock
    private ScheduleRuleProfileRepository scheduleRuleProfileRepository;
    @Mock
    private ConstraintDslVersionManager constraintDslVersionManager;

    private ScheduleRuleProfileApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleRuleProfileApplicationService(
                scheduleRuleProfileRepository,
                constraintDslVersionManager
        );
    }

    @Test
    void shouldCreateDraftWithNextVersion() {
        CreateScheduleRuleProfileCommand command = new CreateScheduleRuleProfileCommand();
        command.setDepartmentId(200L);
        command.setProfileCode("dept-default");
        command.setProfileName("默认规则");
        command.setConstraintDslJson("{\"version\":\"1.0\",\"rules\":[]}");

        when(scheduleRuleProfileRepository.findLatestVersion(200L, "DEPT-DEFAULT")).thenReturn(2);
        when(scheduleRuleProfileRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(100L);
        when(scheduleRuleProfileRepository.findById(100L)).thenReturn(Optional.of(new ScheduleRuleProfile(
                100L,
                200L,
                "DEPT-DEFAULT",
                "默认规则",
                3,
                "DRAFT",
                "{\"version\":\"1.0\",\"rules\":[]}",
                null,
                null,
                null,
                null
        )));

        var dto = service.createDraft(command);

        assertEquals(3, dto.versionNo());
        assertEquals("DEPT-DEFAULT", dto.profileCode());
    }

    @Test
    void shouldPublishAndBumpDslVersion() {
        ScheduleRuleProfile profile = new ScheduleRuleProfile(
                101L,
                200L,
                "DEPT-DEFAULT",
                "默认规则",
                5,
                "DRAFT",
                "{\"version\":\"1.0\"}",
                null,
                null,
                null,
                null
        );
        when(scheduleRuleProfileRepository.findById(101L)).thenReturn(Optional.of(profile));
        when(constraintDslVersionManager.bumpVersion("DEFAULT", 200L)).thenReturn(9L);

        ScheduleRuleProfilePublishResultDTO result = service.publish(101L);

        verify(scheduleRuleProfileRepository).archivePublished(200L, "DEPT-DEFAULT");
        verify(scheduleRuleProfileRepository).updateStatus(101L, "PUBLISHED");
        assertEquals(9L, result.dslVersion());
        assertEquals("PUBLISHED", result.profileStatus());
    }
}
