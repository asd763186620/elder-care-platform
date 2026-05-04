package com.eldercare.gateway.filter;

import com.eldercare.common.constant.HeaderConstants;
import com.eldercare.common.context.LoginUser;
import com.eldercare.common.jwt.JwtUtil;
import com.eldercare.common.log.dto.ApiAccessLogMessage;
import com.eldercare.common.log.producer.LogMessageProducer;
import com.eldercare.common.log.util.JsonLogUtil;
import com.eldercare.common.log.util.TraceIdUtil;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gateway 接口访问日志过滤器。
 * 说明：只采集并异步发送日志，RabbitMQ 发送失败不会影响业务响应。
 */
@Component
public class ApiAccessLogGlobalFilter implements GlobalFilter, Ordered {
    /** 日志生产者 Provider。 */
    private final ObjectProvider<LogMessageProducer> producerProvider;
    /** JWT 工具 Provider，用于日志过滤器排在鉴权前时解析用户信息。 */
    private final ObjectProvider<JwtUtil> jwtUtilProvider;
    /** 慢接口阈值，默认 1000ms。 */
    private final long slowThresholdMs;

    public ApiAccessLogGlobalFilter(ObjectProvider<LogMessageProducer> producerProvider,
                                    ObjectProvider<JwtUtil> jwtUtilProvider,
                                    @Value("${log.api.slow-threshold-ms:1000}") long slowThresholdMs) {
        // 保存日志生产者 Provider，避免 RabbitMQ 未配置时启动失败。
        this.producerProvider = producerProvider;
        // 保存 JWT 工具 Provider，避免日志功能强依赖鉴权实现。
        this.jwtUtilProvider = jwtUtilProvider;
        // 保存慢接口阈值。
        this.slowThresholdMs = slowThresholdMs;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 请求开始时间。
        long start = System.currentTimeMillis();
        // 请求时间。
        LocalDateTime requestTime = LocalDateTime.now();
        // 优先复用上游传入的 traceId，否则生成新的。
        String traceId = resolveTraceId(exchange);
        // 将 traceId 放入 MDC，方便网关自身日志打印。
        MDC.put(TraceIdUtil.MDC_TRACE_ID, traceId);
        // 用于记录异常信息。
        AtomicReference<String> errorRef = new AtomicReference<>();
        // 将 traceId 透传给下游业务服务。
        ServerHttpRequest requestWithTrace = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HeaderConstants.TRACE_ID, traceId))
                .build();
        // 使用带 traceId 的请求继续过滤器链。
        ServerWebExchange tracedExchange = exchange.mutate().request(requestWithTrace).build();
        // 执行后续请求，异常时记录但继续抛出。
        return chain.filter(tracedExchange)
                .doOnError(error -> errorRef.set(JsonLogUtil.truncate(error.getMessage(), 1000)))
                .doFinally(signal -> {
                    // 请求结束时发送接口访问日志。
                    sendLog(tracedExchange, traceId, requestTime, System.currentTimeMillis() - start, errorRef.get());
                    // 清理 MDC，避免 Reactor 线程复用串数据。
                    MDC.remove(TraceIdUtil.MDC_TRACE_ID);
                });
    }

    @Override
    public int getOrder() {
        // 在 JWT 之前执行，才能记录未登录、Token 失效等被鉴权过滤器直接拦截的请求。
        return -200;
    }

    /** 发送接口访问日志。 */
    private void sendLog(ServerWebExchange exchange, String traceId, LocalDateTime requestTime, long costTime, String errorMessage) {
        try {
            // 当前请求。
            ServerHttpRequest request = exchange.getRequest();
            // 响应状态码。
            Integer statusCode = exchange.getResponse().getStatusCode() == null ? null : exchange.getResponse().getStatusCode().value();
            // 构造日志消息。
            LoginUser loginUser = resolveLoginUser(request);
            ApiAccessLogMessage message = new ApiAccessLogMessage(
                    traceId,
                    resolveUserId(request, loginUser),
                    resolveRoleType(request, loginUser),
                    resolveCommunityId(request, loginUser),
                    request.getMethod() == null ? null : request.getMethod().name(),
                    request.getURI().getPath(),
                    JsonLogUtil.truncate(request.getURI().getRawQuery(), 4000),
                    clientIp(exchange),
                    JsonLogUtil.truncate(request.getHeaders().getFirst("User-Agent"), 512),
                    statusCode,
                    costTime,
                    costTime > slowThresholdMs,
                    errorMessage,
                    requestTime
            );
            // 发送日志，失败由 producer 内部处理。
            LogMessageProducer producer = producerProvider.getIfAvailable();
            // producer 为空时跳过。
            if (producer != null) {
                // 异步发送接口日志。
                producer.sendApiAccessLog(message);
            }
        } catch (Exception exception) {
            // 过滤器自身异常不能影响主流程。
            org.slf4j.LoggerFactory.getLogger(ApiAccessLogGlobalFilter.class).error("api access log filter failed", exception);
        }
    }

    /** 获取 traceId。 */
    private String resolveTraceId(ServerWebExchange exchange) {
        // 先读取请求头。
        String traceId = exchange.getRequest().getHeaders().getFirst(HeaderConstants.TRACE_ID);
        // 没有时生成新的。
        return StringUtils.hasText(traceId) ? traceId : TraceIdUtil.newTraceId();
    }

    /** 获取客户端 IP，兼容 Nginx 反向代理。 */
    private String clientIp(ServerWebExchange exchange) {
        // X-Forwarded-For 可能包含多级代理，取第一个。
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        // 存在时取第一个 IP。
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        // 其次读取 X-Real-IP。
        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        // 存在时返回。
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        // 兜底使用远端地址。
        return exchange.getRequest().getRemoteAddress() == null ? "unknown" : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    /** 字符串转 Long。 */
    private Long parseLong(String value) {
        try {
            // 空字符串返回 null。
            return StringUtils.hasText(value) ? Long.valueOf(value) : null;
        } catch (Exception exception) {
            // 格式异常时返回 null。
            return null;
        }
    }

    /** 从请求头或 Authorization 中解析用户 ID。 */
    private Long resolveUserId(ServerHttpRequest request, LoginUser loginUser) {
        // 优先读取后续过滤器透传的用户头。
        Long headerUserId = parseLong(request.getHeaders().getFirst(HeaderConstants.USER_ID));
        // 请求头存在时直接返回。
        if (headerUserId != null) {
            return headerUserId;
        }
        // 兜底读取 JWT 中的用户 ID。
        return loginUser == null ? null : loginUser.userId();
    }

    /** 从请求头或 Authorization 中解析当前角色。 */
    private String resolveRoleType(ServerHttpRequest request, LoginUser loginUser) {
        // 优先读取单角色头。
        String headerRole = request.getHeaders().getFirst(HeaderConstants.USER_ROLE);
        // 存在时返回。
        if (StringUtils.hasText(headerRole)) {
            return headerRole;
        }
        // 其次读取多角色头中的第一个角色。
        String roles = request.getHeaders().getFirst(HeaderConstants.ROLES);
        // 存在时拆分第一个。
        if (StringUtils.hasText(roles)) {
            return roles.split(",")[0].trim();
        }
        // 兜底读取 JWT 中的当前角色。
        return loginUser == null || loginUser.role() == null ? null : loginUser.role().name();
    }

    /** 从请求头或 Authorization 中解析社区 ID。 */
    private Long resolveCommunityId(ServerHttpRequest request, LoginUser loginUser) {
        // 优先读取请求头。
        Long headerCommunityId = parseLong(request.getHeaders().getFirst(HeaderConstants.COMMUNITY_ID));
        // 请求头存在时返回。
        if (headerCommunityId != null) {
            return headerCommunityId;
        }
        // 兜底读取 JWT。
        return loginUser == null ? null : loginUser.communityId();
    }

    /** 从 Authorization 解析登录用户；失败返回 null，不能影响主流程。 */
    private LoginUser resolveLoginUser(ServerHttpRequest request) {
        try {
            // 读取 Authorization 请求头。
            String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            // 只处理 Bearer Token。
            if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
                return null;
            }
            // 获取 JWT 工具。
            JwtUtil jwtUtil = jwtUtilProvider.getIfAvailable();
            // 没有 JWT 工具时返回 null。
            if (jwtUtil == null) {
                return null;
            }
            // 解析 Token。
            return jwtUtil.parseToken(authorization.substring(7));
        } catch (Exception exception) {
            // Token 无效时不影响接口日志记录。
            return null;
        }
    }
}
