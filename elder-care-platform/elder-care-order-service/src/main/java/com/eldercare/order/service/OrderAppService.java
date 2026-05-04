package com.eldercare.order.service;

import com.eldercare.api.dto.OrderCreateDTO;
import com.eldercare.api.dto.OrderEvaluateDTO;
import com.eldercare.api.vo.CursorPageVO;
import com.eldercare.api.vo.OrderDetailVO;
import com.eldercare.api.vo.OrderEvaluationVO;
import com.eldercare.api.vo.OrderStatusCountVO;
import com.eldercare.api.vo.OrderVO;
import com.eldercare.api.vo.VolunteerScoreVO;

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
     * 查询订单详情。
     *
     * @param orderId 订单 ID。
     * @return 订单详情。
     */
    OrderDetailVO detail(Long orderId);

    /**
     * 我的订单游标分页。
     */
    CursorPageVO<OrderVO> myOrdersPage(String status, Long lastId, Integer size);

    /**
     * 公共订单池游标分页。
     */
    CursorPageVO<OrderVO> poolPage(Long serviceItemId, Long lastId, Integer size);

    /**
     * 当前用户订单状态数量。
     */
    OrderStatusCountVO statusCount();

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

    /**
     * 志愿者开始服务。
     */
    void start(Long orderId);

    /**
     * 志愿者提交完成，等待老人或亲情号确认。
     */
    void submitComplete(Long orderId);

    /**
     * 老人或亲情号确认完成。
     */
    void confirm(Long orderId);

    /**
     * 提交订单评价。
     */
    void evaluate(Long orderId, OrderEvaluateDTO dto);

    /**
     * 查询志愿者评价列表。
     */
    CursorPageVO<OrderEvaluationVO> volunteerReviews(Long volunteerId, Long lastId, Integer size);

    /**
     * 查询志愿者评分信息。
     */
    VolunteerScoreVO volunteerScore(Long volunteerId);

    /**
     * 自动关闭超时订单。
     *
     * @return 本次关闭数量。
     */
    int autoCloseTimeoutOrders();
}
