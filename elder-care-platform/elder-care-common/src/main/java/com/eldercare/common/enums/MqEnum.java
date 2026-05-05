package com.eldercare.common.enums;

/**
 * RabbitMQ 交换机、队列和路由键枚举。
 */
public enum MqEnum {
    LOG_EXCHANGE("elder.care.log.exchange", "日志交换机"),
    API_ACCESS_LOG_QUEUE("elder.care.log.api-access.queue", "接口访问日志队列"),
    OPERATION_LOG_QUEUE("elder.care.log.operation.queue", "操作审计日志队列"),
    API_ACCESS_LOG_ROUTING_KEY("log.api-access", "接口访问日志路由键"),
    OPERATION_LOG_ROUTING_KEY("log.operation", "操作审计日志路由键"),
    ORDER_EVENT_EXCHANGE("elder.care.order.event.exchange", "订单事件交换机"),
    ORDER_TIMEOUT_DELAY_EXCHANGE("elder.care.order.timeout.delay.exchange", "订单超时延迟交换机"),
    ORDER_TIMEOUT_DEAD_EXCHANGE("elder.care.order.timeout.dead.exchange", "订单超时死信交换机"),
    ORDER_NOTIFY_QUEUE("elder.care.order.notify.queue", "订单通知队列"),
    ORDER_TIMEOUT_DELAY_QUEUE("elder.care.order.timeout.delay.queue", "订单超时延迟队列"),
    ORDER_TIMEOUT_DEAD_QUEUE("elder.care.order.timeout.dead.queue", "订单超时死信队列"),
    ORDER_CREATED_ROUTING_KEY("order.created", "订单创建路由键"),
    ORDER_TIMEOUT_DELAY_ROUTING_KEY("order.timeout.delay", "订单超时延迟消息路由键"),
    ORDER_TIMEOUT_DEAD_ROUTING_KEY("order.timeout.dead", "订单超时死信消息路由键"),
    ORDER_GRABBED_ROUTING_KEY("order.grabbed", "订单抢单成功路由键"),
    ORDER_CANCELLED_ROUTING_KEY("order.cancelled", "订单取消路由键"),
    ORDER_STARTED_ROUTING_KEY("order.started", "订单开始服务路由键"),
    ORDER_SUBMITTED_ROUTING_KEY("order.submitted", "订单提交完成路由键"),
    ORDER_COMPLETED_ROUTING_KEY("order.completed", "订单完成路由键");

    private final String code;
    private final String message;

    MqEnum(String code, String message) {
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
