package me.jianwen.mediask.infra.schedule.engine.dsl;

import me.jianwen.mediask.domain.cache.LocalCacheService;
import me.jianwen.mediask.schedule.domain.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.engine.SolverConfig;
import me.jianwen.mediask.schedule.domain.engine.SolverStrategy;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleDemand;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleOptimizationRequest;
import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleDoctorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConstraintDslCompilerTest {

    @Mock
    private LocalCacheService localCacheService;
    @Mock
    private ConstraintDslVersionManager versionManager;

    private ConstraintDslCompiler compiler;

    @BeforeEach
    void setUp() {
        compiler = new ConstraintDslCompiler(localCacheService, versionManager);
        when(versionManager.currentVersion(anyString(), any())).thenReturn(0L);
        when(localCacheService.get(any(), anyString(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<Object> loader = invocation.getArgument(2, Supplier.class);
            return loader.get();
        });
    }

    @Test
    void shouldCompileDefaultDslWhenConstraintDslNotProvided() {
        SchedulingEngineRequest request = new SchedulingEngineRequest(
                "DEFAULT",
                200L,
                optimizationRequest(),
                List.of(new ScheduleDoctorProfile(100L, 200L, "主治医师", false)),
                List.of(),
                List.of(),
                new SolverConfig(SolverStrategy.AUTO, 2000, 3000L, 42L),
                null
        );

        var compiled = compiler.compile(request);

        assertEquals("1.0", compiled.version());
        assertFalse(compiled.hardRules().isEmpty());
        assertFalse(compiled.softRules().isEmpty());
        assertNotNull(compiled.hardExpression());
        assertNotNull(compiled.objective());
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
