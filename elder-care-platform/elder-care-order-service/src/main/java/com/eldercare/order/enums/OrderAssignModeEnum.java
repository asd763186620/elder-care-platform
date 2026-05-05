package com.eldercare.order.enums;

import java.util.Arrays;

/**
 * 订单派单模式枚举。
 * 说明：同时覆盖前端入参模式和数据库存储模式，避免 Service 中散落字符串。
 */
public enum OrderAssignModeEnum {
    /** 前端入参：指定志愿者。 */
    ASSIGNED("ASSIGNED", "指定志愿者"),
    /** 前端入参：公共订单池。 */
    PUBLIC("PUBLIC", "公共订单池"),
    /** 数据库存储：直接指定志愿者。 */
    DIRECT("DIRECT", "指定志愿者存储模式"),
    /** 数据库存储：公共订单池。 */
    PUBLIC_POOL("PUBLIC_POOL", "公共订单池存储模式");

    /** 编码。 */
    private final String code;
    /** 说明。 */
    private final String message;

    OrderAssignModeEnum(String code, String message) {
        // 保存编码。
        this.code = code;
        // 保存说明。
        this.message = message;
    }

    public String code() {
        // 返回编码。
        return code;
    }

    public String message() {
        // 返回说明。
        return message;
    }

    /**
     * 判断编码是否等于当前枚举。
     */
    public boolean is(String code) {
        // 使用枚举编码比较，避免业务代码直接写字符串。
        return this.code.equals(code);
    }

    /**
     * 根据编码解析枚举。
     */
    public static OrderAssignModeEnum fromCode(String code) {
        // 遍历匹配编码。
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知派单模式：" + code));
    }
}
