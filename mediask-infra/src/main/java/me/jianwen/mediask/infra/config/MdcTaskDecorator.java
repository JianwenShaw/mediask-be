package me.jianwen.mediask.infra.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * MDC 上下文传播装饰器
 * <p>
 * 用于将父线程的 MDC 上下文（如 traceId、requestUri）传播到异步子线程中，
 * 确保异步任务（如领域事件处理、排班求解）的日志能够关联到原始请求的链路追踪 ID。
 * </p>
 *
 * @author jianwen
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
