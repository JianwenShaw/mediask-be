package me.jianwen.mediask.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.dto.schedule.ScheduleTemplateDTO;
import me.jianwen.mediask.common.dto.schedule.ScheduleTemplateRuleDTO;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.entity.ScheduleTemplate;
import me.jianwen.mediask.schedule.domain.entity.ScheduleTemplateRule;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.repository.ScheduleTemplateRepository;
import me.jianwen.mediask.schedule.domain.service.SlotManagementDomainService;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
import me.jianwen.mediask.service.application.command.CreateScheduleTemplateCommand;
import me.jianwen.mediask.service.application.command.GenerateScheduleFromTemplateCommand;
import me.jianwen.mediask.service.application.command.ScheduleTemplateRuleCommand;
import me.jianwen.mediask.service.application.command.UpdateScheduleTemplateCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleTemplateApplicationService {

    private final ScheduleTemplateRepository scheduleTemplateRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final SlotManagementDomainService slotManagementDomainService;

    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(CreateScheduleTemplateCommand command) {
        validateDateRange(command.getEffectiveStartDate(), command.getEffectiveEndDate());
        validateRules(command.getRules());

        ScheduleTemplate template = new ScheduleTemplate();
        template.setDoctorId(command.getDoctorId());
        template.setTemplateName(command.getTemplateName());
        template.setEffectiveStartDate(command.getEffectiveStartDate());
        template.setEffectiveEndDate(command.getEffectiveEndDate());
        template.setCancelDeadlineMinutes(command.getCancelDeadlineMinutes());
        template.setDefaultFee(command.getDefaultFee());
        template.setStatus(0);
        template.setRules(toRuleEntities(command.getRules(), command.getDefaultFee()));
        return scheduleTemplateRepository.saveTemplate(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long templateId, UpdateScheduleTemplateCommand command) {
        ScheduleTemplate existing = scheduleTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "排班模板不存在"));
        validateDateRange(command.getEffectiveStartDate(), command.getEffectiveEndDate());
        validateRules(command.getRules());

        existing.setTemplateName(command.getTemplateName());
        existing.setEffectiveStartDate(command.getEffectiveStartDate());
        existing.setEffectiveEndDate(command.getEffectiveEndDate());
        existing.setCancelDeadlineMinutes(command.getCancelDeadlineMinutes());
        existing.setDefaultFee(command.getDefaultFee());
        existing.setStatus(command.getStatus() == null ? existing.getStatus() : command.getStatus());
        existing.setRules(toRuleEntities(command.getRules(), command.getDefaultFee()));
        scheduleTemplateRepository.updateTemplate(existing);
    }

    public ScheduleTemplateDTO getTemplate(Long templateId) {
        ScheduleTemplate template = scheduleTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "排班模板不存在"));
        return toDTO(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishTemplate(Long templateId) {
        ScheduleTemplate template = scheduleTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "排班模板不存在"));
        template.setStatus(1);
        scheduleTemplateRepository.updateTemplate(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Long> generateSchedules(GenerateScheduleFromTemplateCommand command) {
        ScheduleTemplate template = scheduleTemplateRepository.findById(command.getTemplateId())
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "排班模板不存在"));
        if (template.getStatus() == null || template.getStatus() != 1) {
            throw new BizException(ErrorCode.OPERATION_FORBIDDEN, "模板未发布，无法生成排班");
        }

        LocalDate startDate = command.getStartDate();
        LocalDate endDate = command.getEndDate();
        validateDateRange(startDate, endDate);

        List<Long> scheduleIds = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (date.isBefore(template.getEffectiveStartDate()) || date.isAfter(template.getEffectiveEndDate())) {
                continue;
            }
            int weekday = mapWeekday(date.getDayOfWeek());
            for (ScheduleTemplateRule rule : template.getRules()) {
                if (!rule.getWeekday().equals(weekday)) {
                    continue;
                }
                TimePeriod timePeriod = TimePeriod.fromCode(rule.getTimePeriodCode());
                DoctorId doctorId = DoctorId.of(template.getDoctorId());
                if (scheduleRepository.exists(doctorId, date, timePeriod)) {
                    continue;
                }

                DoctorSchedule schedule = DoctorSchedule.create(
                        doctorId,
                        date,
                        timePeriod,
                        rule.getSlotCapacity(),
                        rule.getSlotDurationMinutes());
                schedule.setFee(rule.getFee());
                scheduleRepository.save(schedule);
                slotManagementDomainService.saveSlots(slotManagementDomainService.generateSlotsForSchedule(schedule));
                scheduleIds.add(schedule.getId().getValue());
            }
        }
        log.info("模板生成排班完成: templateId={}, count={}", command.getTemplateId(), scheduleIds.size());
        return scheduleIds;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "日期区间非法");
        }
    }

    private void validateRules(List<ScheduleTemplateRuleCommand> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板规则不能为空");
        }
    }

    private List<ScheduleTemplateRule> toRuleEntities(List<ScheduleTemplateRuleCommand> rules, java.math.BigDecimal defaultFee) {
        return rules.stream().map(ruleCommand -> {
            ScheduleTemplateRule rule = new ScheduleTemplateRule();
            rule.setWeekday(ruleCommand.getWeekday());
            rule.setTimePeriodCode(ruleCommand.getTimePeriodCode());
            rule.setPeriodStartTime(ruleCommand.getPeriodStartTime());
            rule.setPeriodEndTime(ruleCommand.getPeriodEndTime());
            rule.setSlotDurationMinutes(ruleCommand.getSlotDurationMinutes());
            rule.setSlotCapacity(ruleCommand.getSlotCapacity());
            rule.setFee(ruleCommand.getFee() == null ? defaultFee : ruleCommand.getFee());
            rule.setStatus(1);
            return rule;
        }).toList();
    }

    private ScheduleTemplateDTO toDTO(ScheduleTemplate template) {
        return ScheduleTemplateDTO.builder()
                .id(template.getId())
                .doctorId(template.getDoctorId())
                .templateName(template.getTemplateName())
                .effectiveStartDate(template.getEffectiveStartDate())
                .effectiveEndDate(template.getEffectiveEndDate())
                .cancelDeadlineMinutes(template.getCancelDeadlineMinutes())
                .defaultFee(template.getDefaultFee())
                .status(template.getStatus())
                .rules(template.getRules().stream().map(rule ->
                        ScheduleTemplateRuleDTO.builder()
                                .id(rule.getId())
                                .weekday(rule.getWeekday())
                                .timePeriodCode(rule.getTimePeriodCode())
                                .periodStartTime(rule.getPeriodStartTime())
                                .periodEndTime(rule.getPeriodEndTime())
                                .slotDurationMinutes(rule.getSlotDurationMinutes())
                                .slotCapacity(rule.getSlotCapacity())
                                .fee(rule.getFee())
                                .build()).toList())
                .build();
    }

    private int mapWeekday(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
            case SUNDAY -> 7;
        };
    }
}
