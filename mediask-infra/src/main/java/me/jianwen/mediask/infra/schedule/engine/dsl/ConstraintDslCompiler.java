package me.jianwen.mediask.infra.schedule.engine.dsl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.util.JsonUtil;
import me.jianwen.mediask.domain.cache.CacheDefinition;
import me.jianwen.mediask.domain.cache.CacheOperations;
import me.jianwen.mediask.schedule.domain.optimization.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.optimization.engine.constraint.CompiledConstraintModel;
import me.jianwen.mediask.schedule.domain.optimization.engine.constraint.ConstraintExpression;
import me.jianwen.mediask.schedule.domain.optimization.engine.constraint.ConstraintKind;
import me.jianwen.mediask.schedule.domain.optimization.engine.constraint.ConstraintRule;
import me.jianwen.mediask.schedule.domain.optimization.engine.constraint.ObjectiveSpec;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JSON DSL 编译器。
 */
@Component
public class ConstraintDslCompiler {

    /**
     * 编译产物缓存定义：仅本地缓存（编译结果是不可变的，按内容哈希+版本号寻址）。
     */
    private static final CacheDefinition COMPILED_DSL_CACHE = CacheDefinition.builder("compiled-dsl")
            .localOnly()
            .localTtl(Duration.ofMinutes(5))
            .localMaxSize(512)
            .build();

    private final CacheOperations cacheOperations;
    private final ConstraintDslVersionManager versionManager;

    public ConstraintDslCompiler(
            CacheOperations cacheOperations,
            ConstraintDslVersionManager versionManager) {
        this.cacheOperations = cacheOperations;
        this.versionManager = versionManager;
    }

    public CompiledConstraintModel compile(SchedulingEngineRequest request) {
        String sourceDsl = request.constraintDslJson();
        if (sourceDsl == null || sourceDsl.isBlank()) {
            sourceDsl = DefaultConstraintDslFactory.build(request.optimizationRequest());
        }
        final String finalSourceDsl = sourceDsl;

        String sourceHash = sha256(finalSourceDsl);
        String namespace = request.namespace() == null || request.namespace().isBlank()
                ? "DEFAULT"
                : request.namespace().trim().toUpperCase(Locale.ROOT);
        long version = versionManager.currentVersion(namespace, request.departmentId());
        String cacheKey = namespace + ":" + request.departmentId() + ":" + version + ":" + sourceHash;

        return cacheOperations.get(COMPILED_DSL_CACHE, cacheKey, CompiledConstraintModel.class,
                () -> doCompile(finalSourceDsl, sourceHash));
    }

    private CompiledConstraintModel doCompile(String sourceDsl, String sourceHash) {
        try {
            JsonNode root = JsonUtil.getObjectMapper().readTree(sourceDsl);
            String version = textValue(root.get("version"), "1.0");

            Map<String, ConstraintRule> hardRules = new HashMap<>();
            Map<String, ConstraintRule> softRules = new HashMap<>();
            JsonNode rulesNode = root.get("rules");
            if (rulesNode == null || !rulesNode.isArray() || rulesNode.isEmpty()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "约束 DSL 缺少 rules 配置");
            }
            for (JsonNode node : rulesNode) {
                ConstraintRule rule = parseRule(node);
                if (!rule.enabled()) {
                    continue;
                }
                if (rule.kind() == ConstraintKind.HARD) {
                    hardRules.put(rule.id(), rule);
                } else {
                    softRules.put(rule.id(), rule);
                }
            }
            if (hardRules.isEmpty()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "约束 DSL 至少需要一个 HARD 规则");
            }

            ConstraintExpression hardExpression = parseExpression(
                    root.get("hardExpr"),
                    hardRules.keySet().stream().sorted().toList()
            );
            ConstraintExpression softExpression = parseExpression(
                    root.get("softExpr"),
                    softRules.keySet().stream().sorted().toList()
            );

            JsonNode objectiveNode = root.get("objective");
            ObjectiveSpec objective = new ObjectiveSpec(
                    objectiveNode == null ? "WEIGHTED_SUM" : textValue(objectiveNode.get("type"), "WEIGHTED_SUM"),
                    parseWeightMap(objectiveNode == null ? null : objectiveNode.get("weights"))
            );

            return new CompiledConstraintModel(
                    version,
                    sourceHash,
                    Map.copyOf(hardRules),
                    Map.copyOf(softRules),
                    hardExpression,
                    softExpression,
                    objective
            );
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.PARAM_INVALID, "约束 DSL 编译失败: " + exception.getMessage());
        }
    }

    private ConstraintRule parseRule(JsonNode node) {
        String id = textValue(node.get("id"), null);
        String kindRaw = textValue(node.get("kind"), "SOFT");
        String type = textValue(node.get("type"), null);
        if (id == null || type == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "约束规则缺少 id/type");
        }
        ConstraintKind kind;
        try {
            kind = ConstraintKind.valueOf(kindRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.PARAM_INVALID, "约束规则 kind 仅支持 HARD/SOFT: " + kindRaw);
        }

        boolean enabled = node.get("enabled") == null || node.get("enabled").asBoolean(true);
        double weight = node.get("weight") == null ? 1D : node.get("weight").asDouble(1D);
        int priority = node.get("priority") == null ? 0 : node.get("priority").asInt(0);
        Map<String, Object> params = node.get("params") == null
                ? Map.of()
                : JsonUtil.getObjectMapper().convertValue(node.get("params"), new TypeReference<Map<String, Object>>() {
        });

        return new ConstraintRule(id, kind, type, enabled, weight, priority, params);
    }

    private ConstraintExpression parseExpression(JsonNode node, List<String> defaultRuleIds) {
        if (node == null || node.isNull()) {
            return defaultAndExpression(defaultRuleIds);
        }
        if (node.hasNonNull("ref")) {
            return new ConstraintExpression.RuleRef(node.get("ref").asText());
        }
        String op = textValue(node.get("op"), null);
        if (op == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "表达式节点缺少 op/ref");
        }

        return switch (op.trim().toUpperCase(Locale.ROOT)) {
            case "AND" -> {
                List<ConstraintExpression> children = new ArrayList<>();
                JsonNode childNode = node.get("children");
                if (childNode == null || !childNode.isArray() || childNode.isEmpty()) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "AND 表达式必须包含 children");
                }
                for (JsonNode child : childNode) {
                    children.add(parseExpression(child, defaultRuleIds));
                }
                yield new ConstraintExpression.And(List.copyOf(children));
            }
            case "OR" -> {
                List<ConstraintExpression> children = new ArrayList<>();
                JsonNode childNode = node.get("children");
                if (childNode == null || !childNode.isArray() || childNode.isEmpty()) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "OR 表达式必须包含 children");
                }
                for (JsonNode child : childNode) {
                    children.add(parseExpression(child, defaultRuleIds));
                }
                yield new ConstraintExpression.Or(List.copyOf(children));
            }
            case "NOT" -> {
                JsonNode childNode = node.get("child");
                if (childNode == null || childNode.isNull()) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "NOT 表达式必须包含 child");
                }
                yield new ConstraintExpression.Not(parseExpression(childNode, defaultRuleIds));
            }
            default -> throw new BizException(ErrorCode.PARAM_INVALID, "不支持的表达式操作符: " + op);
        };
    }

    private ConstraintExpression defaultAndExpression(List<String> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return new ConstraintExpression.And(List.of());
        }
        List<ConstraintExpression> children = new ArrayList<>();
        for (String ruleId : ruleIds) {
            children.add(new ConstraintExpression.RuleRef(ruleId));
        }
        return new ConstraintExpression.And(List.copyOf(children));
    }

    private Map<String, Double> parseWeightMap(JsonNode weightsNode) {
        if (weightsNode == null || weightsNode.isNull()) {
            return Map.of();
        }
        Map<String, Object> raw = JsonUtil.getObjectMapper().convertValue(weightsNode, new TypeReference<Map<String, Object>>() {
        });
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number number) {
                result.put(entry.getKey(), number.doubleValue());
            }
        }
        return Map.copyOf(result);
    }

    private String textValue(JsonNode node, String defaultValue) {
        return node == null || node.isNull() ? defaultValue : node.asText(defaultValue);
    }

    private String sha256(String text) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
