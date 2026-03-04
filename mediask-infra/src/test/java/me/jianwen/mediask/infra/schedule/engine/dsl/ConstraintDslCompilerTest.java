package me.jianwen.mediask.infra.schedule.engine.dsl;

import me.jianwen.mediask.domain.cache.CacheDefinition;
import me.jianwen.mediask.domain.cache.CacheOperations;
import me.jianwen.mediask.schedule.domain.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.engine.SolverConfig;
import me.jianwen.mediask.schedule.domain.engine.SolverStrategy;
import me.jianwen.mediask.schedule.domain.engine.constraint.CompiledConstraintModel;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleDemand;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleOptimizationRequest;
import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleDoctorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConstraintDslCompilerTest {

    @Mock
    private CacheOperations cacheOperations;
    @Mock
    private ConstraintDslVersionManager versionManager;

    private ConstraintDslCompiler compiler;

    /**
     * 简易缓存模拟：key → value 的 HashMap，用于验证缓存语义而非透传 loader。
     */
    private Map<String, Object> cacheStore;

    @BeforeEach
    void setUp() {
        compiler = new ConstraintDslCompiler(cacheOperations, versionManager);
        cacheStore = new HashMap<>();

        when(versionManager.currentVersion(anyString(), any())).thenReturn(0L);

        // 使用 HashMap 模拟真实缓存行为（命中时返回缓存值，未命中时调用 loader 并存入缓存）
        when(cacheOperations.get(any(), anyString(), any(Class.class), any())).thenAnswer(invocation -> {
            String cacheKey = invocation.getArgument(1, String.class);
            if (cacheStore.containsKey(cacheKey)) {
                return cacheStore.get(cacheKey);
            }
            @SuppressWarnings("unchecked")
            Supplier<Object> loader = invocation.getArgument(3, Supplier.class);
            Object result = loader.get();
            cacheStore.put(cacheKey, result);
            return result;
        });
    }

    @Test
    void shouldCompileDefaultDslWhenConstraintDslNotProvided() {
        SchedulingEngineRequest request = createRequest(null);

        var compiled = compiler.compile(request);

        assertEquals("1.0", compiled.version());
        assertFalse(compiled.hardRules().isEmpty());
        assertFalse(compiled.softRules().isEmpty());
        assertNotNull(compiled.hardExpression());
        assertNotNull(compiled.objective());
    }

    @Test
    void shouldReturnCachedResultWhenSameRequestCalledTwice() {
        SchedulingEngineRequest request = createRequest(null);

        CompiledConstraintModel first = compiler.compile(request);
        CompiledConstraintModel second = compiler.compile(request);

        // 第二次应命中缓存，返回同一对象引用
        assertSame(first, second);

        // cacheOperations.get 被调用两次，但 loader 只执行一次（第二次命中缓存）
        verify(cacheOperations, times(2)).get(any(CacheDefinition.class), anyString(), eq(CompiledConstraintModel.class), any());
        assertEquals(1, cacheStore.size(), "缓存中应该只有一个条目");
    }

    @Test
    void shouldRecompileWhenVersionChanges() {
        SchedulingEngineRequest request = createRequest(null);

        // 第一次编译：版本号 0
        when(versionManager.currentVersion(anyString(), any())).thenReturn(0L);
        CompiledConstraintModel v0 = compiler.compile(request);

        // 第二次编译：版本号递增到 1（模拟规则发布）
        when(versionManager.currentVersion(anyString(), any())).thenReturn(1L);
        CompiledConstraintModel v1 = compiler.compile(request);

        // 版本变更后应重新编译（新的 cache key），缓存中应有两个条目
        assertEquals(2, cacheStore.size(), "版本变更应产生新的缓存条目");
        // 两次编译结果内容相同但应是不同对象（各自独立编译）
        assertEquals(v0.version(), v1.version());

        // 验证两次使用了不同的 cache key
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheOperations, times(2)).get(any(CacheDefinition.class), keyCaptor.capture(), eq(CompiledConstraintModel.class), any());
        List<String> keys = keyCaptor.getAllValues();
        assertFalse(keys.get(0).equals(keys.get(1)), "版本变更后 cache key 应不同");
    }

    @Test
    void shouldUseDifferentCacheKeyForDefaultDslAndExplicitDsl() {
        // 默认 DSL（constraintDsl = null）
        SchedulingEngineRequest defaultRequest = createRequest(null);
        compiler.compile(defaultRequest);

        // 显式 DSL
        String explicitDsl = """
                {
                    "version": "1.0",
                    "rules": [
                        {"id": "custom_rule", "kind": "HARD", "type": "MAX_SHIFTS_PER_DAY", "params": {"max": 1}}
                    ],
                    "objective": {"type": "WEIGHTED_SUM", "weights": {}}
                }
                """;
        SchedulingEngineRequest explicitRequest = createRequest(explicitDsl);
        compiler.compile(explicitRequest);

        // 缓存中应有两个不同条目
        assertEquals(2, cacheStore.size(), "默认 DSL 和显式 DSL 应使用不同的 cache key");

        // 验证两次使用了不同的 cache key
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheOperations, times(2)).get(any(CacheDefinition.class), keyCaptor.capture(), eq(CompiledConstraintModel.class), any());
        List<String> keys = keyCaptor.getAllValues();
        assertFalse(keys.get(0).equals(keys.get(1)), "默认 DSL 和显式 DSL 的 cache key 应不同");
    }

    private SchedulingEngineRequest createRequest(String constraintDsl) {
        return new SchedulingEngineRequest(
                "DEFAULT",
                200L,
                optimizationRequest(),
                List.of(new ScheduleDoctorProfile(100L, 200L, "主治医师", false)),
                List.of(),
                List.of(),
                new SolverConfig(SolverStrategy.AUTO, 2000, 3000L, 42L),
                constraintDsl
        );
    }

    private DepartmentScheduleOptimizationRequest optimizationRequest() {
        LocalDate date = LocalDate.of(2026, 2, 20);
        return new DepartmentScheduleOptimizationRequest(
                date,
                date,
                List.of(1, 2),
                List.of(new DepartmentScheduleDemand(200L, date, 1, 1, 0)),
                Set.of(),
                Set.of(),
                new DepartmentScheduleOptimizationRequest.HardConstraints(
                        5,
                        10,
                        12,
                        false,
                        "REDUCED",
                        0.5D
                ),
                new DepartmentScheduleOptimizationRequest.SoftGoals(0.3D, 0.2D, 0.15D, 0.2D, 0.15D)
        );
    }
}
