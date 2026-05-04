package com.eldercare.common.constant;

/**
 * RabbitMQ 常量。
 */
public final class MqConstants {

    /**
     * 订单事件交换机。
     */
    public static final String ORDER_EVENT_EXCHANGE = "elder.care.order.event.exchange";

    /**
     * 订单通知队列。
     */
    public static final String ORDER_NOTIFY_QUEUE = "elder.care.order.notify.queue";

    /**
     * 订单创建路由键。
     */
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    /**
     * 订单抢单成功路由键。
     */
    public static final String ORDER_GRABBED_ROUTING_KEY = "order.grabbed";

    /**
     * 订单取消路由键。
     */
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    /**
     * 订单开始服务路由键。
     */
    public static final String ORDER_STARTED_ROUTING_KEY = "order.started";

    /**
     * 订单提交完成路由键。
     */
    public static final String ORDER_SUBMITTED_ROUTING_KEY = "order.submitted";

    /**
     * 订单完成路由键。
     */
    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.completed";

    /**
     * 私有构造方法。
     */
    private MqConstants() {
    }
}
