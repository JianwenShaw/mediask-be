package me.jianwen.mediask.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.dal.entity.TestConnectionEntity;
import me.jianwen.mediask.dal.mapper.TestConnectionMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 连接测试控制器
 * 用于验证 MySQL 和 Redis 连接是否正常
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestConnectionController {

    private final TestConnectionMapper testConnectionMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    /**
     * 测试 MySQL 连接 - 写入
     */
    @PostMapping("/mysql")
    public Map<String, Object> testMysqlWrite(
            @RequestParam(name = "message", defaultValue = "Hello MediAsk!") String message) {
        Map<String, Object> result = new HashMap<>();
        try {
            TestConnectionEntity entity = new TestConnectionEntity();
            entity.setMessage(message);
            testConnectionMapper.insert(entity);

            result.put("success", true);
            result.put("message", "MySQL 写入成功");
            result.put("data", entity);
            log.info("MySQL 连接测试成功，插入数据: {}", entity);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "MySQL 连接失败: " + e.getMessage());
            log.error("MySQL 连接测试失败", e);
        }
        return result;
    }

    /**
     * 测试 MySQL 连接 - 读取
     */
    @GetMapping("/mysql")
    public Map<String, Object> testMysqlRead() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<TestConnectionEntity> list = testConnectionMapper.selectList(null);
            result.put("success", true);
            result.put("message", "MySQL 读取成功");
            result.put("count", list.size());
            result.put("data", list);
            log.info("MySQL 读取测试成功，共 {} 条记录", list.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "MySQL 连接失败: " + e.getMessage());
            log.error("MySQL 读取测试失败", e);
        }
        return result;
    }

    /**
     * 测试 Redis 连接 - 写入
     */
    @PostMapping("/redis")
    public Map<String, Object> testRedisWrite(
            @RequestParam(name = "key", defaultValue = "test:key") String key,
            @RequestParam(name = "value", defaultValue = "Hello Redis!") String value) {
        Map<String, Object> result = new HashMap<>();
        try {
            stringRedisTemplate.opsForValue().set(key, value, 10, TimeUnit.MINUTES);

            result.put("success", true);
            result.put("message", "Redis 写入成功");
            result.put("key", key);
            result.put("value", value);
            result.put("ttl", "10 minutes");
            log.info("Redis 连接测试成功，写入 key={}, value={}", key, value);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Redis 连接失败: " + e.getMessage());
            log.error("Redis 连接测试失败", e);
        }
        return result;
    }

    /**
     * 测试 Redis 连接 - 读取
     */
    @GetMapping("/redis")
    public Map<String, Object> testRedisRead(@RequestParam(name = "key", defaultValue = "test:key") String key) {
        Map<String, Object> result = new HashMap<>();
        try {
            String value = stringRedisTemplate.opsForValue().get(key);

            result.put("success", true);
            result.put("message", value != null ? "Redis 读取成功" : "Key 不存在");
            result.put("key", key);
            result.put("value", value);
            log.info("Redis 读取测试成功，key={}, value={}", key, value);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Redis 连接失败: " + e.getMessage());
            log.error("Redis 读取测试失败", e);
        }
        return result;
    }

    /**
     * 综合测试 - 同时测试 MySQL 和 Redis
     */
    @GetMapping("/all")
    public Map<String, Object> testAll() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now());

        // 测试 MySQL
        Map<String, Object> mysqlResult = new HashMap<>();
        try {
            long count = testConnectionMapper.selectCount(null);
            mysqlResult.put("status", "UP");
            mysqlResult.put("recordCount", count);
        } catch (Exception e) {
            mysqlResult.put("status", "DOWN");
            mysqlResult.put("error", e.getMessage());
        }
        result.put("mysql", mysqlResult);

        // 测试 Redis
        Map<String, Object> redisResult = new HashMap<>();
        try {
            stringRedisTemplate.opsForValue().set("health:check", "ok", 30, TimeUnit.SECONDS);
            String value = stringRedisTemplate.opsForValue().get("health:check");
            redisResult.put("status", "UP");
            redisResult.put("ping", value);
        } catch (Exception e) {
            redisResult.put("status", "DOWN");
            redisResult.put("error", e.getMessage());
        }
        result.put("redis", redisResult);

        return result;
    }
}
