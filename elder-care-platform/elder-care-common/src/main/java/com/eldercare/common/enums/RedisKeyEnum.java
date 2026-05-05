package com.eldercare.common.enums;

/**
 * Redis Key 前缀枚举。
 */
public enum RedisKeyEnum {
    TOKEN_BLACKLIST("elder-care:auth:token:blacklist:", "登录 Token 黑名单 Key 前缀"),
    REPEAT_SUBMIT("repeat:", "防重复提交 Key 前缀"),
    ORDER_GRAB_LOCK("elder-care:lock:order:grab:", "公共池抢单分布式锁 Key 前缀"),
    VOLUNTEER_TIME_LOCK("elder-care:lock:volunteer:time:", "志愿者时间锁 Key 前缀"),
    LOGIN_CAPTCHA("elder-care:auth:captcha:", "登录验证码 Key 前缀"),
    USER_CONTEXT("elder-care:user:context:", "用户上下文缓存 Key 前缀");

    private final String code;
    private final String message;

    RedisKeyEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
