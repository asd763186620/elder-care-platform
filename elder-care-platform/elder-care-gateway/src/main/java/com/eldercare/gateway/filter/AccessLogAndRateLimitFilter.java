package com.eldercare.gateway.filter;

import com.eldercare.common.constant.HeaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * 网关访问日志、慢接口日志和简单 Redis 限流过滤器。
 */
@Component
public class AccessLogAndRateLimitFilter implements GlobalFilter, Ordered {
    /** 日志对象。 */
    private static final Logger log = LoggerFactory.getLogger(AccessLogAndRateLimitFilter.class);
    /** 慢接口阈值，单位毫秒。 */
    private static final long SLOW_API_MS = 1000L;
    /** Redis 客户端。 */
    private final ReactiveStringRedisTemplate redisTemplate;

    public AccessLogAndRateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        // 保存 Redis 客户端。
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 记录请求开始时间。
        long start = System.currentTimeMillis();
        // 生成 traceId，方便串联一次请求的日志。
        String traceId = UUID.randomUUID().toString();
        // 当前请求。
        ServerHttpRequest request = exchange.getRequest();
        // 根据路径选择限流窗口和阈值。
        RateRule rule = rule(request.getURI().getPath());
        // 没有限流规则时直接进入访问日志链路。
        if (rule == null) {
            // 执行后续过滤器并记录日志。
            return chain.filter(exchange).doFinally(signal -> writeLog(exchange, traceId, start));
        }
        // 构造限流 key。
        String key = "rate:" + rule.keyPrefix() + ":" + clientKey(exchange) + ":" + request.getURI().getPath();
        // Redis incr 计数。
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    // 第一次访问时设置过期时间。
                    Mono<Boolean> expire = count == 1 ? redisTemplate.expire(key, rule.window()) : Mono.just(Boolean.TRUE);
                    // 计数超过阈值时返回 429。
                    if (count > rule.limit()) {
                        // 保证 key 有过期时间后拒绝请求。
                        return expire.then(reject(exchange));
                    }
                    // 未超限则继续后续过滤器。
                    return expire.then(chain.filter(exchange));
                })
                // Redis 异常不能影响主流程，降级为只打日志。
                .onErrorResume(ex -> chain.filter(exchange))
                // 请求结束后记录访问日志和慢接口日志。
                .doFinally(signal -> writeLog(exchange, traceId, start));
    }

    @Override
    public int getOrder() {
        // 在 JWT 之后执行，便于拿到透传用户头；白名单登录接口也能限流。
        return -90;
    }

    /** 根据路径匹配限流规则。 */
    private RateRule rule(String path) {
        // 登录接口按 IP 限流。
        if (path.startsWith("/auth/wx-login")) return new RateRule("login", 30, Duration.ofMinutes(1));
        // 刷新令牌按 IP 或用户限流。
        if (path.startsWith("/auth/refresh-token")) return new RateRule("refresh", 60, Duration.ofMinutes(1));
        // 抢单接口按用户 1 秒最多 3 次。
        if (path.contains("/grab")) return new RateRule("grab", 3, Duration.ofSeconds(1));
        // 下单接口按用户 1 秒最多 5 次。
        if ("/orders".equals(path)) return new RateRule("order-create", 5, Duration.ofSeconds(1));
        // 消息查询按用户 1 分钟最多 120 次。
        if (path.startsWith("/notices/")) return new RateRule("notice", 120, Duration.ofMinutes(1));
        // 无规则不限制。
        return null;
    }

    /** 获取限流主体。 */
    private String clientKey(ServerWebExchange exchange) {
        // 优先使用网关 JWT 透传后的用户 ID。
        String userId = exchange.getRequest().getHeaders().getFirst(HeaderConstants.USER_ID);
        // 已登录时按用户限流。
        if (userId != null && !userId.isBlank()) return "u:" + userId;
        // 未登录时按 IP 限流。
        return "ip:" + clientIp(exchange);
    }

    /** 获取客户端 IP。 */
    private String clientIp(ServerWebExchange exchange) {
        // 优先读取 X-Forwarded-For。
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        // 多级代理取第一个 IP。
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        // 兜底使用远端地址。
        return exchange.getRequest().getRemoteAddress() == null ? "unknown" : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    /** 返回 429。 */
    private Mono<Void> reject(ServerWebExchange exchange) {
        // 设置 HTTP 状态码。
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        // 结束响应。
        return exchange.getResponse().setComplete();
    }

    /** 写访问日志和慢接口日志。 */
    private void writeLog(ServerWebExchange exchange, String traceId, long start) {
        // 计算耗时。
        long cost = System.currentTimeMillis() - start;
        // 当前请求。
        ServerHttpRequest request = exchange.getRequest();
        // 状态码。
        Integer status = exchange.getResponse().getStatusCode() == null ? null : exchange.getResponse().getStatusCode().value();
        // 打印访问日志，不输出 Authorization 等敏感头。
        log.info("api access traceId={} userId={} role={} communityId={} method={} path={} query={} status={} cost={}ms ip={}",
                traceId, request.getHeaders().getFirst(HeaderConstants.USER_ID), request.getHeaders().getFirst(HeaderConstants.USER_ROLE),
                request.getHeaders().getFirst(HeaderConstants.COMMUNITY_ID), request.getMethod(), request.getURI().getPath(),
                request.getURI().getRawQuery(), status, cost, clientIp(exchange));
        // 慢接口单独 warn。
        if (cost > SLOW_API_MS) {
            // 慢接口日志不影响主流程。
            log.warn("slow api traceId={} path={} cost={}ms status={}", traceId, request.getURI().getPath(), cost, status);
        }
    }

    /** 限流规则。 */
    private record RateRule(String keyPrefix, long limit, Duration window) {
    }
}
