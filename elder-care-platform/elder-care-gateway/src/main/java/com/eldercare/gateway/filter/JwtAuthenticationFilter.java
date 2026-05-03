package com.eldercare.gateway.filter;

import com.eldercare.common.constant.HeaderConstants;
import com.eldercare.common.context.LoginUser;
import com.eldercare.common.jwt.JwtUtil;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 全局鉴权过滤器。
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    /**
     * 不需要登录即可访问的路径前缀。
     */
    private static final List<String> WHITE_PATH_PREFIXES = List.of(
            "/auth/mock-login",
            "/auth/wx-login",
            "/auth/refresh-token",
            "/api/user/auth/login",
            "/swagger-ui.html",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/actuator"
    );

    /**
     * JWT 工具类。
     */
    private final JwtUtil jwtUtil;

    /**
     * 构造过滤器。
     *
     * @param jwtUtil JWT 工具类。
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        // 保存 JWT 工具类，后续用于解析 Token。
        this.jwtUtil = jwtUtil;
    }

    /**
     * 对所有经过网关的请求执行鉴权。
     *
     * @param exchange 当前请求上下文。
     * @param chain    网关过滤器链。
     * @return Reactor 异步处理结果。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取当前请求路径。
        String path = exchange.getRequest().getURI().getPath();
        // OPTIONS 预检请求直接放行，由跨域过滤器处理。
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            // 继续执行后续过滤器。
            return chain.filter(exchange);
        }
        // 白名单路径直接放行。
        if (isWhitePath(path)) {
            // 继续执行后续过滤器。
            return chain.filter(exchange);
        }
        // 从 Authorization 请求头中读取 Bearer Token。
        String token = resolveToken(exchange.getRequest().getHeaders());
        // Token 为空时直接返回 401。
        if (!StringUtils.hasText(token)) {
            // 返回统一 401 响应。
            return unauthorized(exchange, "未登录或登录已过期");
        }
        try {
            // 解析 Token 得到登录用户。
            LoginUser loginUser = jwtUtil.parseToken(token);
            // userId、communityId、roles 是下游服务做数据隔离和权限判断的必要字段。
            if (loginUser.userId() == null || loginUser.communityId() == null || loginUser.roles() == null || loginUser.roles().isEmpty()) {
                // Token 缺少关键字段时按无效 Token 处理。
                return unauthorized(exchange, "Token 缺少用户上下文");
            }
            // 多角色使用英文逗号拼接后透传给下游服务。
            String roles = String.join(",", loginUser.roles());
            // 将用户信息写入请求头，透传给下游业务服务。
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(headers -> {
                        // 先移除外部请求伪造的用户上下文请求头。
                        headers.remove(HeaderConstants.USER_ID);
                        // 移除外部请求伪造的社区上下文请求头。
                        headers.remove(HeaderConstants.COMMUNITY_ID);
                        // 移除外部请求伪造的多角色请求头。
                        headers.remove(HeaderConstants.ROLES);
                        // 移除外部请求伪造的旧单角色请求头。
                        headers.remove(HeaderConstants.USER_ROLE);
                        // 移除外部请求伪造的手机号请求头。
                        headers.remove(HeaderConstants.USER_PHONE);
                    })
                    .header(HeaderConstants.USER_ID, String.valueOf(loginUser.userId()))
                    .header(HeaderConstants.COMMUNITY_ID, String.valueOf(loginUser.communityId()))
                    .header(HeaderConstants.ROLES, roles)
                    .header(HeaderConstants.USER_ROLE, loginUser.role() == null ? loginUser.roles().get(0) : loginUser.role().name())
                    .header(HeaderConstants.USER_PHONE, loginUser.phone())
                    .build();
            // 使用带用户头的新请求继续后续过滤器。
            return chain.filter(exchange.mutate().request(request).build());
        } catch (Exception exception) {
            // Token 过期、签名错误、格式错误都按未登录处理。
            return unauthorized(exchange, "Token 无效或已过期");
        }
    }

    /**
     * 获取过滤器执行顺序。
     *
     * @return 越小越早执行。
     */
    @Override
    public int getOrder() {
        // 鉴权过滤器需要尽量早执行。
        return -100;
    }

    /**
     * 判断请求路径是否在白名单内。
     *
     * @param path 请求路径。
     * @return true 表示无需鉴权。
     */
    private boolean isWhitePath(String path) {
        // 只要匹配任一白名单前缀就放行。
        return WHITE_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * 从请求头中提取 Bearer Token。
     *
     * @param headers 请求头集合。
     * @return JWT 字符串；没有合法请求头时返回 null。
     */
    private String resolveToken(HttpHeaders headers) {
        // 读取标准 Authorization 请求头。
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        // 请求头为空时返回 null。
        if (!StringUtils.hasText(authorization)) {
            // 没有登录凭证。
            return null;
        }
        // 只接受 Bearer Token 格式。
        if (!authorization.startsWith("Bearer ")) {
            // 格式不正确。
            return null;
        }
        // 去掉 Bearer 前缀后返回真实 Token。
        return authorization.substring(7);
    }

    /**
     * 返回 401 JSON 响应。
     *
     * @param exchange 当前请求上下文。
     * @param message  错误消息。
     * @return Reactor 异步处理结果。
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        // 设置 HTTP 状态码为 401。
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        // 设置响应体类型为 JSON。
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 组装与 common Result 一致的响应结构。
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        // 将字符串响应体转换为 DataBuffer。
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        // 写出响应并结束请求。
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
