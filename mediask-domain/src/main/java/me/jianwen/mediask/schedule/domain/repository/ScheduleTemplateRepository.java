package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.entity.ScheduleTemplate;

import java.util.Optional;

public interface ScheduleTemplateRepository {

    Long saveTemplate(ScheduleTemplate template);

    void updateTemplate(ScheduleTemplate template);

    Optional<ScheduleTemplate> findById(Long templateId);

    void deleteRulesByTemplateId(Long templateId);
}
