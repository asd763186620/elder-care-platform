package com.eldercare.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 抢单记录实体，对应 order_db.order_grab_record 表。
 * 说明：无论抢单成功还是失败都可以记录，便于定位并发抢单问题。
 */
@TableName("order_grab_record")
public class OrderGrabRecord {
    /** 主键 ID，使用 MySQL 自增策略。 */
    @TableId(type = IdType.AUTO)
    public Long id;

    /** 社区 ID，用于限制志愿者只能抢本社区订单。 */
    public Long communityId;

    /** 被抢的订单 ID。 */
    public Long orderId;

    /** 被抢的订单号，冗余保存便于查询展示。 */
    public String orderNo;

    /** 发起抢单的志愿者用户 ID。 */
    public Long volunteerUserId;

    /** 抢单状态：1 成功，0 失败。 */
    public Integer grabStatus;

    /** 失败原因，例如订单已被抢、时间冲突。 */
    public String failReason;

    /** 请求唯一 ID，后续可用于防重复提交和幂等校验。 */
    public String requestId;
}
