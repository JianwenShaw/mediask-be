package me.jianwen.mediask.common.sensitive;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.util.Objects;

/**
 * 敏感数据脱敏序列化器
 * <p>
 * 配合 @Sensitive 注解使用，在 JSON 序列化时自动进行脱敏处理。
 * </p>
 *
 * @author jianwen
 */
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveStrategy strategy;

    public SensitiveSerializer() {
    }

    public SensitiveSerializer(SensitiveStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(strategy.desensitize(value));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return prov.findNullValueSerializer(null);
        }

        // 只处理 String 类型
        if (!Objects.equals(property.getType().getRawClass(), String.class)) {
            return prov.findValueSerializer(property.getType(), property);
        }

        // 获取注解
        Sensitive sensitive = property.getAnnotation(Sensitive.class);
        if (sensitive == null) {
            sensitive = property.getContextAnnotation(Sensitive.class);
        }

        if (sensitive != null) {
            return new SensitiveSerializer(sensitive.strategy());
        }

        return prov.findValueSerializer(property.getType(), property);
    }
}
