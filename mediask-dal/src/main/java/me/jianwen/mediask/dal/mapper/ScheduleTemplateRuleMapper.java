package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.ScheduleTemplateRuleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScheduleTemplateRuleMapper extends BaseMapper<ScheduleTemplateRuleDO> {

    int physicalDeleteByTemplateId(@Param("templateId") Long templateId);
}
