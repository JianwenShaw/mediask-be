package me.jianwen.mediask.infra.diagnostic;

import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.TestConnectionDO;
import me.jianwen.mediask.dal.mapper.TestConnectionMapper;
import me.jianwen.mediask.infra.cache.CaffeineCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 连接测试基础设施服务
 */
@Service
@RequiredArgsConstructor
public class TestConnectionInfraService {

    /**
     * 测试连接键前缀
     */
    private static final String TEST_KEY_PREFIX = "test:connection:";

    private final TestConnectionMapper testConnectionMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CaffeineCacheManager caffeineCacheManager;

    public TestConnectionDO insertMessage(String message) {
        TestConnectionDO dataObject = new TestConnectionDO();
        dataObject.setMessage(message);
        testConnectionMapper.insert(dataObject);
        return dataObject;
    }

    public List<TestConnectionDO> listMessages() {
        return testConnectionMapper.selectList(null);
    }

    public long countMessages() {
        return testConnectionMapper.selectCount(null);
    }

    public void setRedisValue(String key, String value, long timeout, TimeUnit unit) {
        String cacheKey = TEST_KEY_PREFIX + key;
        stringRedisTemplate.opsForValue().set(cacheKey, value, timeout, unit);
    }

    public String getRedisValue(String key) {
        String cacheKey = TEST_KEY_PREFIX + key;
        return stringRedisTemplate.opsForValue().get(cacheKey);
    }

    public Map<String, ?> getLocalCacheStats() {
        return caffeineCacheManager.allStats();
    }
}
