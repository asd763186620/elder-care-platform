package com.eldercare.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 抢单记录实体，对应 order_db.order_grab_record 表。
 */
@TableName("order_grab_record")
public class OrderGrabRecord {
    /** 主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 社区 ID。 */
    private Long communityId;
    /** 被抢的订单 ID。 */
    private Long orderId;
    /** 被抢的订单号。 */
    private String orderNo;
    /** 发起抢单的志愿者用户 ID。 */
    private Long volunteerUserId;
    /** 抢单状态：1 成功，0 失败。 */
    private Integer grabStatus;
    /** 失败原因。 */
    private String failReason;
    /** 请求唯一 ID。 */
    private String requestId;

    /** 创建成功抢单记录。 */
    public static OrderGrabRecord success(ServiceOrder order, Long volunteerUserId, String requestId) {
        OrderGrabRecord record = new OrderGrabRecord();
        record.communityId = order.getCommunityId();
        record.orderId = order.getId();
        record.orderNo = order.getOrderNo();
        record.volunteerUserId = volunteerUserId;
        record.grabStatus = 1;
        record.requestId = requestId;
        return record;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getVolunteerUserId() { return volunteerUserId; }
    public void setVolunteerUserId(Long volunteerUserId) { this.volunteerUserId = volunteerUserId; }
    public Integer getGrabStatus() { return grabStatus; }
    public void setGrabStatus(Integer grabStatus) { this.grabStatus = grabStatus; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
