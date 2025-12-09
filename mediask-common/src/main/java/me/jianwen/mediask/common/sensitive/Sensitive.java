package me.jianwen.mediask.common.sensitive;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感数据脱敏注解
 * <p>
 * 用于标注需要脱敏的字段，在 JSON 序列化时自动进行脱敏处理。
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * @Sensitive(strategy = SensitiveStrategy.PHONE)
 * private String phone;
 *
 * @Sensitive(strategy = SensitiveStrategy.ID_CARD)
 * private String idCard;
 * }
 * </pre>
 *
 * @author jianwen
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {

    /**
     * 脱敏策略
     *
     * @return 脱敏策略枚举
     */
    SensitiveStrategy strategy();
}
