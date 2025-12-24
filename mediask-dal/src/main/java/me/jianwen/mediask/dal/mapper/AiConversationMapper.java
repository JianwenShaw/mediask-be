package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.AiConversationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话会话Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversationDO> {
}
