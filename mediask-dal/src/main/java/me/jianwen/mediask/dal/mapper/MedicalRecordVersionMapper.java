package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.MedicalRecordVersionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 病历版本历史Mapper
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Mapper
public interface MedicalRecordVersionMapper extends BaseMapper<MedicalRecordVersionDO> {
}
