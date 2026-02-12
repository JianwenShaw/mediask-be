package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.ScheduleTemplateDO;
import me.jianwen.mediask.dal.entity.ScheduleTemplateRuleDO;
import me.jianwen.mediask.dal.mapper.ScheduleTemplateMapper;
import me.jianwen.mediask.dal.mapper.ScheduleTemplateRuleMapper;
import me.jianwen.mediask.infra.persistence.converter.ScheduleTemplateConverter;
import me.jianwen.mediask.schedule.domain.entity.ScheduleTemplate;
import me.jianwen.mediask.schedule.domain.entity.ScheduleTemplateRule;
import me.jianwen.mediask.schedule.domain.repository.ScheduleTemplateRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ScheduleTemplateRepositoryImpl implements ScheduleTemplateRepository {

    private final ScheduleTemplateMapper templateMapper;
    private final ScheduleTemplateRuleMapper templateRuleMapper;
    private final ScheduleTemplateConverter templateConverter;

    @Override
    public Long saveTemplate(ScheduleTemplate template) {
        ScheduleTemplateDO templateDO = templateConverter.toTemplateDO(template);
        templateMapper.insert(templateDO);

        if (template.getRules() != null) {
            for (ScheduleTemplateRule rule : template.getRules()) {
                rule.setTemplateId(templateDO.getId());
                if (rule.getStatus() == null) {
                    rule.setStatus(1);
                }
                templateRuleMapper.insert(templateConverter.toRuleDO(rule));
            }
        }
        return templateDO.getId();
    }

    @Override
    public void updateTemplate(ScheduleTemplate template) {
        ScheduleTemplateDO templateDO = templateConverter.toTemplateDO(template);
        templateMapper.updateById(templateDO);

        deleteRulesByTemplateId(template.getId());
        if (template.getRules() != null) {
            for (ScheduleTemplateRule rule : template.getRules()) {
                rule.setTemplateId(template.getId());
                if (rule.getStatus() == null) {
                    rule.setStatus(1);
                }
                templateRuleMapper.insert(templateConverter.toRuleDO(rule));
            }
        }
    }

    @Override
    public Optional<ScheduleTemplate> findById(Long templateId) {
        ScheduleTemplateDO templateDO = templateMapper.selectById(templateId);
        if (templateDO == null) {
            return Optional.empty();
        }

        LambdaQueryWrapper<ScheduleTemplateRuleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleTemplateRuleDO::getTemplateId, templateId).orderByAsc(ScheduleTemplateRuleDO::getWeekday);
        List<ScheduleTemplateRuleDO> ruleList = templateRuleMapper.selectList(wrapper);

        ScheduleTemplate template = templateConverter.toTemplateDomain(templateDO);
        template.setRules(ruleList.stream().map(templateConverter::toRuleDomain).toList());
        return Optional.of(template);
    }

    @Override
    public void deleteRulesByTemplateId(Long templateId) {
        templateRuleMapper.physicalDeleteByTemplateId(templateId);
    }
}
