package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
