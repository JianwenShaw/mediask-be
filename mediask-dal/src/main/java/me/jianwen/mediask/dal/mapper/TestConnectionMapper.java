package me.jianwen.mediask.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.dal.entity.TestConnectionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 连接测试 Mapper
 */
@Mapper
public interface TestConnectionMapper extends BaseMapper<TestConnectionEntity> {
}
