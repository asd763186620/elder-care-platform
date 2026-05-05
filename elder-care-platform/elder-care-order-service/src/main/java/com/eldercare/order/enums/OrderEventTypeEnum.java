package com.eldercare.order.enums;

/**
 * 订单事件类型枚举。
 */
public enum OrderEventTypeEnum {
    /** 订单创建。 */
    ORDER_CREATED("ORDER_CREATED", "订单创建"),
    /** 抢单成功。 */
    ORDER_GRABBED("ORDER_GRABBED", "抢单成功"),
    /** 订单取消。 */
    ORDER_CANCELLED("ORDER_CANCELLED", "订单取消"),
    /** 开始服务。 */
    ORDER_STARTED("ORDER_STARTED", "开始服务"),
    /** 提交完成。 */
    ORDER_SUBMITTED("ORDER_SUBMITTED", "提交完成"),
    /** 确认完成。 */
    ORDER_COMPLETED("ORDER_COMPLETED", "确认完成");

    /** 事件编码。 */
    private final String code;
    /** 事件说明。 */
    private final String message;

    OrderEventTypeEnum(String code, String message) {
        // 保存事件编码。
        this.code = code;
        // 保存事件说明。
        this.message = message;
    }

    public String code() {
        // 返回事件编码。
        return code;
    }

    public String message() {
        // 返回事件说明。
        return message;
    }
}
