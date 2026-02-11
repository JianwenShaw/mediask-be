package me.jianwen.mediask.api.controller;

import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.service.application.service.TestConnectionApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 连接测试控制器
 * 用于验证 MySQL 和 Redis 连接是否正常
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestConnectionController {

    private final TestConnectionApplicationService testConnectionApplicationService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(testConnectionApplicationService.health());
    }

    /**
     * 测试 MySQL 连接 - 写入
     */
    @PostMapping("/mysql")
    public Result<Map<String, Object>> testMysqlWrite(
            @RequestParam(name = "message", defaultValue = "Hello MediAsk!") String message) {
        return toResult(testConnectionApplicationService.testMysqlWrite(message));
    }

    /**
     * 测试 MySQL 连接 - 读取
     */
    @GetMapping("/mysql")
    public Result<Map<String, Object>> testMysqlRead() {
        return toResult(testConnectionApplicationService.testMysqlRead());
    }

    /**
     * 测试 Redis 连接 - 写入
     */
    @PostMapping("/redis")
    public Result<Map<String, Object>> testRedisWrite(
            @RequestParam(name = "key", defaultValue = "test:key") String key,
            @RequestParam(name = "value", defaultValue = "Hello Redis!") String value) {
        return toResult(testConnectionApplicationService.testRedisWrite(key, value));
    }

    /**
     * 测试 Redis 连接 - 读取
     */
    @GetMapping("/redis")
    public Result<Map<String, Object>> testRedisRead(@RequestParam(name = "key", defaultValue = "test:key") String key) {
        return toResult(testConnectionApplicationService.testRedisRead(key));
    }

    /**
     * 综合测试 - 同时测试 MySQL 和 Redis
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> testAll() {
        return toResult(testConnectionApplicationService.testAll());
    }

    private Result<Map<String, Object>> toResult(Map<String, Object> payload) {
        Object success = payload.get("success");
        if (Boolean.FALSE.equals(success)) {
            return Result.fail(String.valueOf(payload.getOrDefault("message", "连接测试失败")));
        }
        return Result.ok(payload);
    }
}
