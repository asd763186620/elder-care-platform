package com.eldercare.order.enums;

import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态枚举。
 * 说明：第二版开始不再在业务代码里散落订单状态字符串，所有状态值统一从这里获取。
 */
public enum OrderStatusEnum {
    /** 待分配，生产语义下的公共池待抢单状态。 */
    PENDING_ASSIGN,
    /** 已分配，生产语义下已指定或已抢单成功状态。 */
    ASSIGNED,
    /** 待抢单，公共池订单等待志愿者抢单。 */
    WAIT_GRAB,
    /** 待服务，已经指定志愿者或抢单成功。 */
    WAIT_SERVICE,
    /** 服务中，志愿者已经点击开始服务。 */
    IN_SERVICE,
    /** 待确认，志愿者提交完成，等待老人或亲情号确认。 */
    WAIT_CONFIRM,
    /** 已完成，老人或亲情号已确认。 */
    COMPLETED,
    /** 已取消，用户主动取消。 */
    CANCELLED,
    /** 超时关闭，系统自动关闭。 */
    TIMEOUT_CLOSED;

    /**
     * 合法状态流转表。
     */
    private static final Map<OrderStatusEnum, Set<OrderStatusEnum>> TRANSITIONS = Map.of(
            PENDING_ASSIGN, Set.of(ASSIGNED, CANCELLED, TIMEOUT_CLOSED),
            ASSIGNED, Set.of(IN_SERVICE, CANCELLED, TIMEOUT_CLOSED),
            WAIT_GRAB, Set.of(WAIT_SERVICE, CANCELLED, TIMEOUT_CLOSED),
            WAIT_SERVICE, Set.of(IN_SERVICE, CANCELLED, TIMEOUT_CLOSED),
            IN_SERVICE, Set.of(WAIT_CONFIRM),
            WAIT_CONFIRM, Set.of(COMPLETED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of(),
            TIMEOUT_CLOSED, Set.of()
    );

    /**
     * 返回数据库保存的状态值。
     *
     * @return 状态编码。
     */
    public String code() {
        // 当前枚举名就是数据库状态编码。
        return name();
    }

    /**
     * 校验状态流转是否合法。
     *
     * @param from 当前状态。
     * @param to   目标状态。
     */
    public static void checkCanTransit(String from, OrderStatusEnum to) {
        // 当前状态为空只允许创建订单时写初始状态。
        if (from == null) {
            // 创建场景直接放行。
            return;
        }
        // 将数据库状态转换成枚举。
        OrderStatusEnum fromEnum = fromCode(from);
        // 目标状态必须在合法流转集合中。
        if (!TRANSITIONS.getOrDefault(fromEnum, Set.of()).contains(to)) {
            // 非法流转直接抛业务异常。
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不能从 " + from + " 流转到 " + to.code());
        }
    }

    /**
     * 按数据库状态值解析枚举。
     *
     * @param code 状态编码。
     * @return 状态枚举。
     */
    public static OrderStatusEnum fromCode(String code) {
        // 遍历枚举并匹配名称。
        for (OrderStatusEnum value : values()) {
            // 找到相同编码后返回。
            if (value.code().equals(code)) {
                // 返回匹配状态。
                return value;
            }
        }
        // 未识别状态按数据异常处理。
        throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "未知订单状态：" + code);
    }
}
