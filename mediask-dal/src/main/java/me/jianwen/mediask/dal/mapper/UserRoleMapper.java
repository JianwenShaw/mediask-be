package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.UserRoleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleDO> {
}
