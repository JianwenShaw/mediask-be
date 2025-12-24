package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.HospitalDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医院Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface HospitalMapper extends BaseMapper<HospitalDO> {
}
