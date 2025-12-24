package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.AiMessageDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话消息Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessageDO> {
}
