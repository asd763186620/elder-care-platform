package com.eldercare.common.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性，从 application.yml 或 Nacos 配置中心读取。
 */
@Component
@ConfigurationProperties(prefix = "elder-care.jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥，HS256 至少需要 32 字节长度。
     */
    private String secret = "elder-care-platform-jwt-secret-demo-please-change";

    /**
     * Token 过期秒数，默认 7 天。
     */
    private Long expireSeconds = 604800L;

    /**
     * 获取 JWT 签名密钥。
     *
     * @return 签名密钥。
     */
    public String getSecret() {
        // 返回当前配置的签名密钥。
        return secret;
    }

    /**
     * 设置 JWT 签名密钥。
     *
     * @param secret 签名密钥。
     */
    public void setSecret(String secret) {
        // 保存外部配置传入的签名密钥。
        this.secret = secret;
    }

    /**
     * 获取 Token 过期秒数。
     *
     * @return 过期秒数。
     */
    public Long getExpireSeconds() {
        // 返回当前配置的过期时间。
        return expireSeconds;
    }

    /**
     * 设置 Token 过期秒数。
     *
     * @param expireSeconds 过期秒数。
     */
    public void setExpireSeconds(Long expireSeconds) {
        // 保存外部配置传入的过期时间。
        this.expireSeconds = expireSeconds;
    }
}
