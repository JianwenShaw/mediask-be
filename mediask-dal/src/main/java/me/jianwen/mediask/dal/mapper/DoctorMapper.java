package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.DoctorDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医生Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface DoctorMapper extends BaseMapper<DoctorDO> {
}
