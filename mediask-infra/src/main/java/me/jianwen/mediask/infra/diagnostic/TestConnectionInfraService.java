package me.jianwen.mediask.infra.diagnostic;

import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.TestConnectionDO;
import me.jianwen.mediask.dal.mapper.TestConnectionMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 连接测试基础设施服务
 */
@Service
@RequiredArgsConstructor
public class TestConnectionInfraService {

    private final TestConnectionMapper testConnectionMapper;
    private final StringRedisTemplate stringRedisTemplate;

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
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String getRedisValue(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }
}
