package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.DrugDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 药品字典Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface DrugMapper extends BaseMapper<DrugDO> {
}
