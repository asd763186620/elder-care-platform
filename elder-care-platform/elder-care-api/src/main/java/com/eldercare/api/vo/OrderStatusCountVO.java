package com.eldercare.api.vo;

/**
 * 我的订单状态数量。
 *
 * @param waitGrabCount    待抢单数量。
 * @param waitServiceCount 待服务数量。
 * @param inServiceCount   服务中数量。
 * @param waitConfirmCount 待确认数量。
 * @param completedCount   已完成数量。
 * @param cancelledCount   已取消和超时关闭数量。
 */
public record OrderStatusCountVO(long waitGrabCount,
                                 long waitServiceCount,
                                 long inServiceCount,
                                 long waitConfirmCount,
                                 long completedCount,
                                 long cancelledCount) {
}
