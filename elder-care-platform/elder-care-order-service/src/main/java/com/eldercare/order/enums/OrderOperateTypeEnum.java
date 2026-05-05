package com.eldercare.order.enums;

/**
 * 订单操作类型枚举，用于 order_status_log.operate_type。
 */
public enum OrderOperateTypeEnum {
    CREATE("CREATE", "创建订单"),
    GRAB("GRAB", "抢单"),
    CANCEL("CANCEL", "取消订单"),
    START("START", "开始服务"),
    SUBMIT_COMPLETE("SUBMIT_COMPLETE", "提交完成"),
    CONFIRM_COMPLETE("CONFIRM_COMPLETE", "确认完成"),
    AUTO_TIMEOUT_CANCEL("AUTO_TIMEOUT_CANCEL", "超时自动关闭");

    private final String code;
    private final String message;

    OrderOperateTypeEnum(String code, String message) {
        // 保存操作编码。
        this.code = code;
        // 保存操作说明。
        this.message = message;
    }

    public String code() {
        // 返回操作编码。
        return code;
    }

    public String message() {
        // 返回操作说明。
        return message;
    }
}
