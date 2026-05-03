package com.eldercare.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 网关统一跨域配置。
 */
@Configuration
public class CorsConfig {

    /**
     * 注册 CORS 过滤器。
     *
     * @return CORS 过滤器。
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        // 创建跨域配置对象。
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 允许所有来源，第一版便于小程序、H5 和本地调试接入。
        corsConfiguration.addAllowedOriginPattern("*");
        // 允许所有请求头，包括 Authorization 和自定义透传头。
        corsConfiguration.addAllowedHeader("*");
        // 允许常见 HTTP 方法。
        corsConfiguration.addAllowedMethod("*");
        // 允许前端携带 Cookie 或认证信息。
        corsConfiguration.setAllowCredentials(true);
        // 预检请求缓存 1 小时。
        corsConfiguration.setMaxAge(3600L);
        // 创建基于路径的 CORS 配置源。
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 所有网关路径都使用同一套跨域规则。
        source.registerCorsConfiguration("/**", corsConfiguration);
        // 返回 Spring WebFlux CORS 过滤器。
        return new CorsWebFilter(source);
    }

    /**
     * 处理 OPTIONS 预检请求，避免进入 JWT 鉴权逻辑。
     *
     * @return WebFlux 过滤器。
     */
    @Bean
    public WebFilter optionsRequestWebFilter() {
        // 返回一个只处理 OPTIONS 请求的过滤器。
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            // 获取当前请求。
            ServerHttpRequest request = exchange.getRequest();
            // 非 OPTIONS 请求继续后续过滤器。
            if (request.getMethod() != HttpMethod.OPTIONS) {
                // 放行普通请求。
                return chain.filter(exchange);
            }
            // 获取当前响应。
            ServerHttpResponse response = exchange.getResponse();
            // 设置允许的请求头。
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
            // 设置允许的请求方法。
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "*");
            // 设置允许的请求来源。
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, request.getHeaders().getOrigin() == null ? "*" : request.getHeaders().getOrigin());
            // 设置预检缓存时间。
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            // 预检请求直接返回 204。
            response.setStatusCode(HttpStatus.NO_CONTENT);
            // 结束响应。
            return Mono.empty();
        };
    }
}
