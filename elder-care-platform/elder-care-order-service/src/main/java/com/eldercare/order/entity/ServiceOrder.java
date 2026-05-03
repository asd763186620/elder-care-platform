package com.eldercare.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 预约单实体，对应 order_db.service_order 表。
 * 说明：这是第一版主链路的核心业务表，保存下单人、老人、服务项目、志愿者和状态。
 */
@TableName("service_order")
public class ServiceOrder {
    /** 主键 ID，使用 MySQL 自增策略。 */
    @TableId(type = IdType.AUTO)
    public Long id;

    /** 社区 ID，所有订单查询和更新都必须带该字段做数据隔离。 */
    public Long communityId;

    /** 订单号，用于展示、日志和通知，不直接暴露数据库主键。 */
    public String orderNo;

    /** 被服务老人用户 ID；老人自己下单时通常等于 creatorUserId。 */
    public Long elderUserId;

    /** 创建订单的用户 ID，可能是老人本人，也可能是亲情号。 */
    public Long creatorUserId;

    /** 创建订单的用户角色，例如 ELDER 或 FAMILY。 */
    public String creatorRole;

    /** 服务项目 ID，对应 community_db.service_item.id。 */
    public Long serviceItemId;

    /** 服务地址，第一版直接记录用户填写的地址文本。 */
    public String serviceAddress;

    /** 预约服务开始时间。 */
    public LocalDateTime serviceStartTime;

    /** 预约服务结束时间。 */
    public LocalDateTime serviceEndTime;

    /** 派单模式：ASSIGNED 指定志愿者，PUBLIC 公共订单池。 */
    public String assignMode;

    /** 用户下单时指定的志愿者 ID，仅 ASSIGNED 模式必填。 */
    public Long specifiedVolunteerUserId;

    /** 最终接单的志愿者 ID，指定派单或抢单成功后写入。 */
    public Long assignedVolunteerUserId;

    /** 订单状态，例如 WAIT_GRAB、ACCEPTED、CANCELLED、COMPLETED。 */
    public String orderStatus;

    /** 订单来源，例如 MINI_PROGRAM，便于后续区分小程序、后台等来源。 */
    public String orderSource;

    /** 下单备注，保存老人或亲情号补充说明。 */
    public String remark;

    /** 取消原因，订单取消时填写。 */
    public String cancelReason;

    /** 接单时间，指定派单创建成功或公共池抢单成功时写入。 */
    public LocalDateTime assignedAt;

    /** 完成时间，订单完成时写入。 */
    public LocalDateTime completedAt;

    /** 取消时间，订单取消时写入。 */
    public LocalDateTime cancelledAt;

    /** 乐观锁版本号，抢单和状态流转时递增，防止并发覆盖。 */
    public Integer version;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    public Integer deleted;
}
