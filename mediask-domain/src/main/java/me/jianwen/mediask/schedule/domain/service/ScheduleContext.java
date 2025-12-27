package me.jianwen.mediask.schedule.domain.service;

import lombok.Builder;
import lombok.Data;
import me.jianwen.mediask.schedule.domain.rule.ScheduleRule;

import java.util.Map;

/**
 * 排班上下文
 * 包含执行排班策略所需的所有配置和规则
 *
 * @author jianwen
 */
@Data
@Builder
public class ScheduleContext {

    /**
     * 排班规则
     */
    private ScheduleRule scheduleRule;

    /**
     * 扩展配置
     */
    private Map<String, Object> properties;

    /**
     * 获取配置属性
     */
    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    /**
     * 获取配置属性（带默认值）
     */
    public <T> T getProperty(String key, Class<T> type, T defaultValue) {
        T value = getProperty(key, type);
        return value != null ? value : defaultValue;
    }
}
