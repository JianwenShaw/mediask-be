package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.MedicalRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电子病历Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface MedicalRecordMapper extends BaseMapper<MedicalRecordDO> {
}
