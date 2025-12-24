package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.PrescriptionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 处方Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface PrescriptionMapper extends BaseMapper<PrescriptionDO> {
}
