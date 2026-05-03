package com.eldercare.order.service;

import com.eldercare.api.dto.OrderCreateDTO;
import com.eldercare.api.vo.OrderVO;

import java.util.List;

/**
 * 订单业务接口。
 */
public interface OrderAppService {
    /**
     * 发布预约单。
     *
     * @param dto 创建订单请求。
     * @return 创建后的订单信息。
     */
    OrderVO create(OrderCreateDTO dto);

    /**
     * 查询我的订单。
     *
     * @return 当前用户相关订单列表。
     */
    List<OrderVO> myOrders();

    /**
     * 查询本社区公共订单池。
     *
     * @return 待抢单订单列表。
     */
    List<OrderVO> pool();

    /**
     * 志愿者抢单。
     *
     * @param orderId 订单 ID。
     */
    void grab(Long orderId);

    /**
     * 取消订单。
     *
     * @param orderId 订单 ID。
     */
    void cancel(Long orderId);

    /**
     * 完成订单。
     *
     * @param orderId 订单 ID。
     */
    void complete(Long orderId);
}
