package com.eldercare.common.constant;

/**
 * Redis Key 前缀常量。
 */
public final class RedisKeyConstants {

    /**
     * 登录 Token 黑名单 Key 前缀。
     */
    public static final String TOKEN_BLACKLIST = "elder-care:auth:token:blacklist:";

    /**
     * 防重复提交 Key 前缀。
     */
    public static final String REPEAT_SUBMIT = "repeat:";

    /**
     * 公共池抢单分布式锁 Key 前缀。
     */
    public static final String ORDER_GRAB_LOCK = "elder-care:lock:order:grab:";

    /**
     * 志愿者时间锁 Key 前缀。
     */
    public static final String VOLUNTEER_TIME_LOCK = "elder-care:lock:volunteer:time:";

    /**
     * 登录验证码 Key 前缀。
     */
    public static final String LOGIN_CAPTCHA = "elder-care:auth:captcha:";

    /**
     * 用户上下文缓存 Key 前缀。
     */
    public static final String USER_CONTEXT = "elder-care:user:context:";

    /**
     * 私有构造方法，防止常量类被实例化。
     */
    private RedisKeyConstants() {
        // 常量类不需要创建对象。
    }
}
