package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.DoctorScheduleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 医生排班Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorScheduleDO> {

    /**
     * 扣减号源（使用乐观锁）
     *
     * @param scheduleId 排班ID
     * @return 更新行数
     */
    int decreaseSlots(@Param("scheduleId") Long scheduleId);
}
