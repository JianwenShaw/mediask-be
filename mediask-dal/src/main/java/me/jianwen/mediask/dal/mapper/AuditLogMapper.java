package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.AuditLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogDO> {
}
