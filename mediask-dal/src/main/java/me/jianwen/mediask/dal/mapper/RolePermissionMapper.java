package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.RolePermissionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色权限关联Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermissionDO> {
}
