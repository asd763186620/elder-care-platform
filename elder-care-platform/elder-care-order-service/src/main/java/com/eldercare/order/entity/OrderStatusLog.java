package com.eldercare.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 订单状态日志实体，对应 order_db.order_status_log 表。
 */
@TableName("order_status_log")
public class OrderStatusLog {
    /** 主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 社区 ID。 */
    private Long communityId;
    /** 订单主键 ID。 */
    private Long orderId;
    /** 订单号。 */
    private String orderNo;
    /** 变更前状态。 */
    private String fromStatus;
    /** 变更后状态。 */
    private String toStatus;
    /** 操作人用户 ID。 */
    private Long operatorUserId;
    /** 操作人角色。 */
    private String operatorRole;
    /** 操作类型。 */
    private String operateType;
    /** 操作说明。 */
    private String operateRemark;

    /** 创建状态日志。 */
    public static OrderStatusLog of(ServiceOrder order, String fromStatus, String toStatus, Long operatorUserId, String operateType) {
        OrderStatusLog log = new OrderStatusLog();
        log.communityId = order.getCommunityId();
        log.orderId = order.getId();
        log.orderNo = order.getOrderNo();
        log.fromStatus = fromStatus;
        log.toStatus = toStatus;
        log.operatorUserId = operatorUserId;
        log.operateType = operateType;
        return log;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public Long getOperatorUserId() { return operatorUserId; }
    public void setOperatorUserId(Long operatorUserId) { this.operatorUserId = operatorUserId; }
    public String getOperatorRole() { return operatorRole; }
    public void setOperatorRole(String operatorRole) { this.operatorRole = operatorRole; }
    public String getOperateType() { return operateType; }
    public void setOperateType(String operateType) { this.operateType = operateType; }
    public String getOperateRemark() { return operateRemark; }
    public void setOperateRemark(String operateRemark) { this.operateRemark = operateRemark; }
}
