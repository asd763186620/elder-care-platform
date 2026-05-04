package com.eldercare.common.log.aspect;

import com.eldercare.common.constant.HeaderConstants;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.context.UserInfoDTO;
import com.eldercare.common.log.annotation.OperationLog;
import com.eldercare.common.log.dto.OperationLogMessage;
import com.eldercare.common.log.producer.LogMessageProducer;
import com.eldercare.common.log.util.JsonLogUtil;
import com.eldercare.common.log.util.TraceIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志 AOP。
 * 说明：只负责采集和发送日志，不吞异常、不改变业务方法返回值。
 */
@Aspect
@Component
@ConditionalOnClass(name = "jakarta.servlet.http.HttpServletRequest")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OperationLogAspect {
    /** 参数名发现器，用于 SpEL 根据参数名取值。 */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    /** SpEL 解析器。 */
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    /** 日志消息生产者，可能在部分服务中不存在。 */
    private final ObjectProvider<LogMessageProducer> producerProvider;
    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    public OperationLogAspect(ObjectProvider<LogMessageProducer> producerProvider, ObjectMapper objectMapper) {
        // 保存生产者 Provider，避免 Rabbit 未配置时启动失败。
        this.producerProvider = producerProvider;
        // 保存 JSON 序列化器。
        this.objectMapper = objectMapper;
    }

    /**
     * 拦截带 @OperationLog 的业务方法。
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        // 记录开始时间。
        long start = System.currentTimeMillis();
        // 方法执行结果。
        Object result = null;
        // 方法是否成功。
        boolean success = false;
        // 异常信息。
        String errorMessage = null;
        try {
            // 执行业务方法。
            result = joinPoint.proceed();
            // 执行到这里表示成功。
            success = true;
            // 返回原始业务结果。
            return result;
        } catch (Throwable throwable) {
            // 记录异常信息。
            errorMessage = JsonLogUtil.truncate(throwable.getMessage(), 1000);
            // 异常必须继续抛出，不能改变原业务行为。
            throw throwable;
        } finally {
            // finally 中保证成功和失败都记录日志。
            sendOperationLog(joinPoint, operationLog, result, success, errorMessage, System.currentTimeMillis() - start);
        }
    }

    /**
     * 组装并发送操作日志。
     */
    private void sendOperationLog(ProceedingJoinPoint joinPoint, OperationLog operationLog, Object result, boolean success, String errorMessage, long costTime) {
        try {
            // 获取当前用户上下文。
            UserInfoDTO user = loadUser();
            // 获取方法签名。
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            // 获取目标方法。
            Method method = signature.getMethod();
            // 解析 traceId。
            String traceId = traceId();
            // 解析 bizId。
            String bizId = resolveBizId(operationLog.bizId(), method, joinPoint.getArgs());
            // 根据配置决定是否记录请求参数。
            String params = operationLog.recordParams() ? JsonLogUtil.toSafeJson(joinPoint.getArgs(), objectMapper, JsonLogUtil.DEFAULT_MAX_LENGTH) : null;
            // 根据配置决定是否记录响应结果。
            String response = operationLog.recordResult() ? JsonLogUtil.toSafeJson(result, objectMapper, JsonLogUtil.DEFAULT_MAX_LENGTH) : null;
            // 构造操作日志消息。
            OperationLogMessage message = new OperationLogMessage(
                    traceId,
                    user == null ? null : user.userId(),
                    primaryRole(user),
                    user == null ? null : user.communityId(),
                    operationLog.module().name(),
                    operationLog.operationType().name(),
                    operationLog.description(),
                    bizId,
                    joinPoint.getTarget().getClass().getName(),
                    method.getName(),
                    params,
                    response,
                    success,
                    errorMessage,
                    costTime,
                    LocalDateTime.now()
            );
            // 发送日志消息，发送失败由 producer 内部吞掉。
            LogMessageProducer producer = producerProvider.getIfAvailable();
            // producer 不存在时直接跳过，避免影响业务。
            if (producer != null) {
                // 发送操作日志。
                producer.sendOperationLog(message);
            }
        } catch (Exception exception) {
            // AOP 自身异常不能影响业务。
            org.slf4j.LoggerFactory.getLogger(OperationLogAspect.class).error("operation log aspect failed", exception);
        }
    }

    /**
     * 解析 SpEL 获取业务 ID。
     */
    private String resolveBizId(String expression, Method method, Object[] args) {
        // 未配置表达式时返回 null。
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        try {
            // 创建 SpEL 上下文。
            EvaluationContext context = new StandardEvaluationContext();
            // 获取参数名。
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            // 参数名存在时写入上下文。
            if (parameterNames != null) {
                // 遍历参数。
                for (int i = 0; i < parameterNames.length; i++) {
                    // 绑定参数名到参数值。
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            // 解析表达式。
            Object value = expressionParser.parseExpression(expression).getValue(context);
            // 转成字符串。
            return value == null ? null : String.valueOf(value);
        } catch (Exception exception) {
            // SpEL 解析失败时返回表达式文本，避免影响业务。
            return expression;
        }
    }

    /**
     * 加载当前用户上下文。
     */
    private UserInfoDTO loadUser() {
        try {
            // 尝试从当前请求头加载。
            UserContext.loadFromCurrentRequest();
            // 返回 ThreadLocal 中的用户。
            return UserContext.get();
        } catch (Exception exception) {
            // 无用户上下文时返回 null。
            return null;
        }
    }

    /**
     * 当前主角色。
     */
    private String primaryRole(UserInfoDTO user) {
        // 用户或角色为空时返回 null。
        if (user == null || user.roles() == null || user.roles().isEmpty()) {
            return null;
        }
        // 返回第一个角色。
        return user.roles().get(0);
    }

    /**
     * 获取 traceId。
     */
    private String traceId() {
        // 优先从 MDC 读取。
        String traceId = MDC.get(TraceIdUtil.MDC_TRACE_ID);
        // MDC 中存在时返回。
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }
        // 尝试从请求头读取。
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 请求上下文存在时读取 X-Trace-Id。
        if (attributes != null) {
            // 当前请求。
            HttpServletRequest request = attributes.getRequest();
            // 请求头中的 traceId。
            String headerTraceId = request.getHeader(HeaderConstants.TRACE_ID);
            // 存在时返回。
            if (StringUtils.hasText(headerTraceId)) {
                return headerTraceId;
            }
        }
        // 兜底生成一个。
        return TraceIdUtil.newTraceId();
    }
}
