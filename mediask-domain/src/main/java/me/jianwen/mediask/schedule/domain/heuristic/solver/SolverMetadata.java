package me.jianwen.mediask.schedule.domain.heuristic.solver;

import lombok.Builder;
import lombok.Data;

/**
 * 求解器元信息
 *
 * <p>描述求解器的特性：
 * <ul>
 *   <li>速度等级</li>
 *   <li>质量等级</li>
 *   <li>适用规模</li>
 * </ul>
 *
 * @author MediAsk
 */
@Data
@Builder
public class SolverMetadata {

    /**
     * 速度等级 (1=快, 5=慢)
     */
    @Builder.Default
    private int speedLevel = 3;

    /**
     * 质量等级 (1=低, 5=高)
     */
    @Builder.Default
    private int qualityLevel = 3;

    /**
     * 是否可解释
     */
    @Builder.Default
    private boolean explainable = true;

    /**
     * 适用规模: SMALL, MEDIUM, LARGE
     */
    @Builder.Default
    private String suitableScale = "ALL";

    /**
     * 是否需要参数调优
     */
    @Builder.Default
    private boolean requiresTuning = false;

    /**
     * 默认超时时间（毫秒）
     */
    @Builder.Default
    private long defaultTimeoutMs = 30000;

    /**
     * 创建快速求解器元信息
     */
    public static SolverMetadata fast() {
        return SolverMetadata.builder()
                .speedLevel(5)
                .qualityLevel(2)
                .explainable(true)
                .suitableScale("ALL")
                .defaultTimeoutMs(5000)
                .build();
    }

    /**
     * 创建高质量求解器元信息
     */
    public static SolverMetadata highQuality() {
        return SolverMetadata.builder()
                .speedLevel(2)
                .qualityLevel(5)
                .explainable(true)
                .suitableScale("SMALL,MEDIUM")
                .requiresTuning(true)
                .defaultTimeoutMs(60000)
                .build();
    }

    /**
     * 创建平衡型求解器元信息
     */
    public static SolverMetadata balanced() {
        return SolverMetadata.builder()
                .speedLevel(3)
                .qualityLevel(4)
                .explainable(true)
                .suitableScale("ALL")
                .defaultTimeoutMs(30000)
                .build();
    }
}
