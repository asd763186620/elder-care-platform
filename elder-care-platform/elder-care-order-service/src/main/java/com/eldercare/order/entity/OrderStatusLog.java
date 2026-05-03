package com.eldercare.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 订单状态日志实体，对应 order_db.order_status_log 表。
 * 说明：订单每次关键状态变更都写一条日志，便于追踪和排查问题。
 */
@TableName("order_status_log")
public class OrderStatusLog {
    /** 主键 ID，使用 MySQL 自增策略。 */
    @TableId(type = IdType.AUTO)
    public Long id;

    /** 社区 ID，用于日志查询时继续保持数据隔离。 */
    public Long communityId;

    /** 订单主键 ID。 */
    public Long orderId;

    /** 订单号，冗余保存便于日志列表展示。 */
    public String orderNo;

    /** 变更前状态，创建订单时可以为空。 */
    public String fromStatus;

    /** 变更后状态。 */
    public String toStatus;

    /** 操作人用户 ID。 */
    public Long operatorUserId;

    /** 操作人角色，例如 ELDER、FAMILY、VOLUNTEER。 */
    public String operatorRole;

    /** 操作类型，例如 CREATE、GRAB、CANCEL、COMPLETE。 */
    public String operateType;

    /** 操作说明或取消原因。 */
    public String operateRemark;
}
