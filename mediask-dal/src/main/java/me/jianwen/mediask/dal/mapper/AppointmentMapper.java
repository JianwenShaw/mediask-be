package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.AppointmentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 挂号预约Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface AppointmentMapper extends BaseMapper<AppointmentDO> {
}
