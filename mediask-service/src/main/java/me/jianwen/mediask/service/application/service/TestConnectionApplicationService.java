package me.jianwen.mediask.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.infra.diagnostic.TestConnectionInfraService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 连接测试应用服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TestConnectionApplicationService {

    private final TestConnectionInfraService testConnectionInfraService;

    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now());
        result.put("success", true);
        return result;
    }

    public Map<String, Object> testMysqlWrite(String message) {
        Map<String, Object> result = new HashMap<>();
        try {
            var dataObject = testConnectionInfraService.insertMessage(message);
            result.put("success", true);
            result.put("message", "MySQL 写入成功");
            result.put("data", dataObject);
            log.info("MySQL 连接测试成功，插入数据: {}", dataObject);
        } catch (Exception ex) {
            result.put("success", false);
            result.put("message", "MySQL 连接失败: " + ex.getMessage());
            log.error("MySQL 连接测试失败", ex);
        }
        return result;
    }

    public Map<String, Object> testMysqlRead() {
        Map<String, Object> result = new HashMap<>();
        try {
            var dataList = testConnectionInfraService.listMessages();
            result.put("success", true);
            result.put("message", "MySQL 读取成功");
            result.put("count", dataList.size());
            result.put("data", dataList);
            log.info("MySQL 读取测试成功，共 {} 条记录", dataList.size());
        } catch (Exception ex) {
            result.put("success", false);
            result.put("message", "MySQL 连接失败: " + ex.getMessage());
            log.error("MySQL 读取测试失败", ex);
        }
        return result;
    }

    public Map<String, Object> testRedisWrite(String key, String value) {
        Map<String, Object> result = new HashMap<>();
        try {
            testConnectionInfraService.setRedisValue(key, value, 10, TimeUnit.MINUTES);
            result.put("success", true);
            result.put("message", "Redis 写入成功");
            result.put("key", key);
            result.put("value", value);
            result.put("ttl", "10 minutes");
            log.info("Redis 连接测试成功，写入 key={}, value={}", key, value);
        } catch (Exception ex) {
            result.put("success", false);
            result.put("message", "Redis 连接失败: " + ex.getMessage());
            log.error("Redis 连接测试失败", ex);
        }
        return result;
    }

    public Map<String, Object> testRedisRead(String key) {
        Map<String, Object> result = new HashMap<>();
        try {
            String value = testConnectionInfraService.getRedisValue(key);
            result.put("success", true);
            result.put("message", value != null ? "Redis 读取成功" : "Key 不存在");
            result.put("key", key);
            result.put("value", value);
            log.info("Redis 读取测试成功，key={}, value={}", key, value);
        } catch (Exception ex) {
            result.put("success", false);
            result.put("message", "Redis 连接失败: " + ex.getMessage());
            log.error("Redis 读取测试失败", ex);
        }
        return result;
    }

    public Map<String, Object> testAll() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now());

        Map<String, Object> mysqlResult = new HashMap<>();
        try {
            long count = testConnectionInfraService.countMessages();
            mysqlResult.put("status", "UP");
            mysqlResult.put("recordCount", count);
        } catch (Exception ex) {
            mysqlResult.put("status", "DOWN");
            mysqlResult.put("error", ex.getMessage());
        }
        result.put("mysql", mysqlResult);

        Map<String, Object> redisResult = new HashMap<>();
        try {
            testConnectionInfraService.setRedisValue("health:check", "ok", 30, TimeUnit.SECONDS);
            String value = testConnectionInfraService.getRedisValue("health:check");
            redisResult.put("status", "UP");
            redisResult.put("ping", value);
        } catch (Exception ex) {
            redisResult.put("status", "DOWN");
            redisResult.put("error", ex.getMessage());
        }
        result.put("redis", redisResult);
        result.put("localCache", testConnectionInfraService.getLocalCacheStats());
        result.put("success", "UP".equals(mysqlResult.get("status")) && "UP".equals(redisResult.get("status")));
        return result;
    }
}
