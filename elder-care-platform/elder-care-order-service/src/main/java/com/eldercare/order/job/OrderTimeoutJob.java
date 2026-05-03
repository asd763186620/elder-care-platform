package com.eldercare.order.job;

import com.eldercare.order.service.OrderAppService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理任务。
 */
@Component
public class OrderTimeoutJob {
    /** 订单业务服务。 */
    private final OrderAppService orderAppService;

    public OrderTimeoutJob(OrderAppService orderAppService) {
        // 保存订单业务服务。
        this.orderAppService = orderAppService;
    }

    /**
     * 每分钟扫描一次超时未抢单订单。
     */
    @Scheduled(fixedDelayString = "${elder-care.order.timeout-scan-delay-ms:60000}")
    public void scanTimeoutOrders() {
        // 扫描并自动关闭超时订单。
        orderAppService.autoCloseTimeoutOrders();
    }
}
