package com.eldercare.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序登录配置。
 */
@Component
@ConfigurationProperties(prefix = "elder-care.wechat-mini-app")
public class WechatMiniAppProperties {
    /** 小程序 appId。 */
    private String appId;
    /** 小程序 appSecret。 */
    private String secret;
    /** 是否启用本地模拟微信返回。 */
    private boolean mockEnabled;

    public String getAppId() {
        // 返回 appId。
        return appId;
    }

    public void setAppId(String appId) {
        // 设置 appId。
        this.appId = appId;
    }

    public String getSecret() {
        // 返回 appSecret。
        return secret;
    }

    public void setSecret(String secret) {
        // 设置 appSecret。
        this.secret = secret;
    }

    public boolean isMockEnabled() {
        // 返回是否开启 mock。
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        // 设置是否开启 mock。
        this.mockEnabled = mockEnabled;
    }
}
