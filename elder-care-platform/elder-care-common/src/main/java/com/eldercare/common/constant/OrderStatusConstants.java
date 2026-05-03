package com.eldercare.common.constant;

/**
 * 预约单状态常量。
 */
public final class OrderStatusConstants {

    /**
     * 已创建，等待系统分配或进入公共池。
     */
    public static final String CREATED = "CREATED";

    /**
     * 公共订单池待抢单。
     */
    public static final String WAITING_GRAB = "WAITING_GRAB";

    /**
     * 已分配志愿者。
     */
    public static final String ASSIGNED = "ASSIGNED";

    /**
     * 服务进行中。
     */
    public static final String IN_SERVICE = "IN_SERVICE";

    /**
     * 服务已完成。
     */
    public static final String COMPLETED = "COMPLETED";

    /**
     * 订单已取消。
     */
    public static final String CANCELLED = "CANCELLED";

    /**
     * 私有构造方法，防止常量类被实例化。
     */
    private OrderStatusConstants() {
        // 常量类不需要创建对象。
    }
}
