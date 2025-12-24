package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.KnowledgeDocumentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentDO> {
}
