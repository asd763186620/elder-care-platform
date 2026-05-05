package com.eldercare.order.enums;

/**
 * 订单超时规则枚举。
 */
public enum OrderTimeoutEnum {
    /** 公共池订单 30 分钟未抢单自动关闭。 */
    PUBLIC_POOL_GRAB_TIMEOUT("PUBLIC_POOL_GRAB_TIMEOUT", "公共池订单30分钟未抢单自动关闭", 30L, "系统超时自动关闭");

    private final String code;
    private final String message;
    private final long timeoutMinutes;
    private final String cancelReason;

    OrderTimeoutEnum(String code, String message, long timeoutMinutes, String cancelReason) {
        this.code = code;
        this.message = message;
        this.timeoutMinutes = timeoutMinutes;
        this.cancelReason = cancelReason;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public long timeoutMinutes() {
        return timeoutMinutes;
    }

    public long timeoutMillis() {
        return timeoutMinutes * 60L * 1000L;
    }

    public String cancelReason() {
        return cancelReason;
    }
}
