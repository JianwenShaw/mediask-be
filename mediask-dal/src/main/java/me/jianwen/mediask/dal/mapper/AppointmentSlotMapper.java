package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.AppointmentSlotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 号源时段Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface AppointmentSlotMapper extends BaseMapper<AppointmentSlotDO> {

    /**
     * 原子占用号源
     */
    int occupySlot(@Param("slotId") Long slotId, @Param("appointmentId") Long appointmentId);

    /**
     * 原子释放号源（仅当属于当前预约时）
     */
    int releaseSlot(@Param("slotId") Long slotId, @Param("appointmentId") Long appointmentId);
}
