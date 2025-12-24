package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.DepartmentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 科室Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<DepartmentDO> {
}
