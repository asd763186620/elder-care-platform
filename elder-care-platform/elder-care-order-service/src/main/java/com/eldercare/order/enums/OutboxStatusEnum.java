package com.eldercare.order.enums;

/**
 * Outbox 本地消息状态枚举。
 */
public enum OutboxStatusEnum {
    INIT("INIT", "待发送"),
    SENT("SENT", "已发送"),
    FAILED("FAILED", "发送失败");

    private final String code;
    private final String message;

    OutboxStatusEnum(String code, String message) {
        // 保存状态编码。
        this.code = code;
        // 保存状态说明。
        this.message = message;
    }

    public String code() {
        // 返回状态编码。
        return code;
    }

    public String message() {
        // 返回状态说明。
        return message;
    }
}
