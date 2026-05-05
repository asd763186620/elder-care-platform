package com.eldercare.order.enums;

/**
 * 订单来源枚举。
 */
public enum OrderSourceEnum {
    /** 小程序下单。 */
    MINI_APP("MINI_APP", "小程序");

    private final String code;
    private final String message;

    OrderSourceEnum(String code, String message) {
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
