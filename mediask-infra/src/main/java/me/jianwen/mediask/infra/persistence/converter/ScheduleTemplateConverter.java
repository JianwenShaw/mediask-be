package me.jianwen.mediask.infra.persistence.converter;

import me.jianwen.mediask.dal.entity.ScheduleTemplateDO;
import me.jianwen.mediask.dal.entity.ScheduleTemplateRuleDO;
import me.jianwen.mediask.schedule.domain.entity.ScheduleTemplate;
import me.jianwen.mediask.schedule.domain.entity.ScheduleTemplateRule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScheduleTemplateConverter {

    default ScheduleTemplateDO toTemplateDO(ScheduleTemplate template) {
        if (template == null) {
            return null;
        }
        ScheduleTemplateDO dataObject = new ScheduleTemplateDO();
        dataObject.setId(template.getId());
        dataObject.setDoctorId(template.getDoctorId());
        dataObject.setTemplateName(template.getTemplateName());
        dataObject.setEffectiveStartDate(template.getEffectiveStartDate());
        dataObject.setEffectiveEndDate(template.getEffectiveEndDate());
        dataObject.setCancelDeadlineMinutes(template.getCancelDeadlineMinutes());
        dataObject.setDefaultFee(template.getDefaultFee());
        dataObject.setStatus(template.getStatus());
        dataObject.setVersion(template.getVersion());
        dataObject.setCreatedAt(template.getCreatedAt());
        dataObject.setUpdatedAt(template.getUpdatedAt());
        return dataObject;
    }

    default ScheduleTemplate toTemplateDomain(ScheduleTemplateDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        ScheduleTemplate template = new ScheduleTemplate();
        template.setId(dataObject.getId());
        template.setDoctorId(dataObject.getDoctorId());
        template.setTemplateName(dataObject.getTemplateName());
        template.setEffectiveStartDate(dataObject.getEffectiveStartDate());
        template.setEffectiveEndDate(dataObject.getEffectiveEndDate());
        template.setCancelDeadlineMinutes(dataObject.getCancelDeadlineMinutes());
        template.setDefaultFee(dataObject.getDefaultFee());
        template.setStatus(dataObject.getStatus());
        template.setVersion(dataObject.getVersion());
        template.setCreatedAt(dataObject.getCreatedAt());
        template.setUpdatedAt(dataObject.getUpdatedAt());
        return template;
    }

    default ScheduleTemplateRuleDO toRuleDO(ScheduleTemplateRule rule) {
        if (rule == null) {
            return null;
        }
        ScheduleTemplateRuleDO dataObject = new ScheduleTemplateRuleDO();
        dataObject.setId(rule.getId());
        dataObject.setTemplateId(rule.getTemplateId());
        dataObject.setWeekday(rule.getWeekday());
        dataObject.setTimePeriod(rule.getTimePeriodCode());
        dataObject.setPeriodStartTime(rule.getPeriodStartTime());
        dataObject.setPeriodEndTime(rule.getPeriodEndTime());
        dataObject.setSlotDurationMinutes(rule.getSlotDurationMinutes());
        dataObject.setSlotCapacity(rule.getSlotCapacity());
        dataObject.setFee(rule.getFee());
        dataObject.setStatus(rule.getStatus());
        return dataObject;
    }

    default ScheduleTemplateRule toRuleDomain(ScheduleTemplateRuleDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        ScheduleTemplateRule rule = new ScheduleTemplateRule();
        rule.setId(dataObject.getId());
        rule.setTemplateId(dataObject.getTemplateId());
        rule.setWeekday(dataObject.getWeekday());
        rule.setTimePeriodCode(dataObject.getTimePeriod());
        rule.setPeriodStartTime(dataObject.getPeriodStartTime());
        rule.setPeriodEndTime(dataObject.getPeriodEndTime());
        rule.setSlotDurationMinutes(dataObject.getSlotDurationMinutes());
        rule.setSlotCapacity(dataObject.getSlotCapacity());
        rule.setFee(dataObject.getFee());
        rule.setStatus(dataObject.getStatus());
        return rule;
    }
}
