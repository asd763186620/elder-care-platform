package com.eldercare.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDateTime;

/**
 * 预约单实体，对应 order_db.service_order 表。
 * 说明：实体不再暴露 public 字段，而是通过方法表达“创建订单、接单、变更状态”等业务行为。
 */
@TableName("service_order")
public class ServiceOrder {
    /** 主键 ID，使用 MySQL 自增策略。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 社区 ID，所有订单查询和更新都必须带该字段做数据隔离。 */
    private Long communityId;

    /** 订单号，用于展示、日志和通知，不直接暴露数据库主键。 */
    private String orderNo;

    /** 被服务老人用户 ID。 */
    private Long elderUserId;

    /** 创建订单的用户 ID，可能是老人本人，也可能是亲情号。 */
    private Long creatorUserId;

    /** 创建订单的用户角色，例如 ELDER 或 FAMILY。 */
    private String creatorRole;

    /** 服务项目 ID，对应 community_db.service_item.id。 */
    private Long serviceItemId;

    /** 服务地址，第一版直接记录用户填写的地址文本。 */
    private String serviceAddress;

    /** 预约服务开始时间。 */
    private LocalDateTime serviceStartTime;

    /** 预约服务结束时间。 */
    private LocalDateTime serviceEndTime;

    /** 派单模式：DIRECT 指定志愿者，PUBLIC_POOL 公共订单池。 */
    private String assignMode;

    /** 用户下单时指定的志愿者 ID，仅 DIRECT 模式必填。 */
    private Long specifiedVolunteerUserId;

    /** 最终接单的志愿者 ID，指定派单或抢单成功后写入。 */
    private Long assignedVolunteerUserId;

    /** 订单状态，例如 WAIT_GRAB、WAIT_SERVICE、IN_SERVICE、WAIT_CONFIRM、COMPLETED。 */
    private String orderStatus;

    /** 订单来源，例如 MINI_APP。 */
    private String orderSource;

    /** 下单备注，保存老人或亲情号补充说明。 */
    private String remark;

    /** 取消原因，订单取消时填写。 */
    private String cancelReason;

    /** 公共池订单抢单截止时间，超过后由定时任务自动关闭。 */
    private LocalDateTime grabDeadline;

    /** 接单时间，指定派单创建成功或公共池抢单成功时写入。 */
    private LocalDateTime assignedAt;

    /** 开始服务时间，志愿者点击开始服务时写入。 */
    private LocalDateTime serviceStartedAt;

    /** 提交完成时间，志愿者提交完成等待确认时写入。 */
    private LocalDateTime submittedAt;

    /** 完成时间，订单完成时写入。 */
    private LocalDateTime completedAt;

    /** 取消时间，订单取消时写入。 */
    private LocalDateTime cancelledAt;

    /** 乐观锁版本号，抢单和状态流转时递增，防止并发覆盖。 */
    @Version
    private Integer version;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    @TableLogic
    private Integer deleted;

    /**
     * 创建订单基础对象。
     */
    public static ServiceOrder createBase(Long communityId, String orderNo, Long elderUserId, Long creatorUserId, String creatorRole,
                                          Long serviceItemId, String serviceAddress, LocalDateTime serviceStartTime,
                                          LocalDateTime serviceEndTime, String remark) {
        ServiceOrder order = new ServiceOrder();
        order.communityId = communityId;
        order.orderNo = orderNo;
        order.elderUserId = elderUserId;
        order.creatorUserId = creatorUserId;
        order.creatorRole = creatorRole;
        order.serviceItemId = serviceItemId;
        order.serviceAddress = serviceAddress;
        order.serviceStartTime = serviceStartTime;
        order.serviceEndTime = serviceEndTime;
        order.remark = remark;
        order.orderSource = "MINI_APP";
        order.version = 0;
        order.deleted = 0;
        return order;
    }

    /** 将订单设置为指定志愿者订单。 */
    public void assignToVolunteer(Long volunteerUserId, String directMode, String acceptedStatus) {
        this.assignMode = directMode;
        this.specifiedVolunteerUserId = volunteerUserId;
        this.assignedVolunteerUserId = volunteerUserId;
        this.orderStatus = acceptedStatus;
        this.assignedAt = LocalDateTime.now();
    }

    /** 将订单设置为公共订单池订单。 */
    public void waitForGrab(String publicPoolMode, String waitGrabStatus, LocalDateTime grabDeadline) {
        this.assignMode = publicPoolMode;
        this.orderStatus = waitGrabStatus;
        this.grabDeadline = grabDeadline;
    }

    /** 标记当前内存对象已经被指定志愿者抢单成功。 */
    public void markGrabbed(Long volunteerUserId, String acceptedStatus) {
        this.assignedVolunteerUserId = volunteerUserId;
        this.orderStatus = acceptedStatus;
        this.assignedAt = LocalDateTime.now();
    }

    /** 标记当前内存对象状态已发生流转。 */
    public void markStatusChanged(String status) {
        this.orderStatus = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getElderUserId() { return elderUserId; }
    public void setElderUserId(Long elderUserId) { this.elderUserId = elderUserId; }
    public Long getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(Long creatorUserId) { this.creatorUserId = creatorUserId; }
    public String getCreatorRole() { return creatorRole; }
    public void setCreatorRole(String creatorRole) { this.creatorRole = creatorRole; }
    public Long getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(Long serviceItemId) { this.serviceItemId = serviceItemId; }
    public String getServiceAddress() { return serviceAddress; }
    public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }
    public LocalDateTime getServiceStartTime() { return serviceStartTime; }
    public void setServiceStartTime(LocalDateTime serviceStartTime) { this.serviceStartTime = serviceStartTime; }
    public LocalDateTime getServiceEndTime() { return serviceEndTime; }
    public void setServiceEndTime(LocalDateTime serviceEndTime) { this.serviceEndTime = serviceEndTime; }
    public String getAssignMode() { return assignMode; }
    public void setAssignMode(String assignMode) { this.assignMode = assignMode; }
    public Long getSpecifiedVolunteerUserId() { return specifiedVolunteerUserId; }
    public void setSpecifiedVolunteerUserId(Long specifiedVolunteerUserId) { this.specifiedVolunteerUserId = specifiedVolunteerUserId; }
    public Long getAssignedVolunteerUserId() { return assignedVolunteerUserId; }
    public void setAssignedVolunteerUserId(Long assignedVolunteerUserId) { this.assignedVolunteerUserId = assignedVolunteerUserId; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getOrderSource() { return orderSource; }
    public void setOrderSource(String orderSource) { this.orderSource = orderSource; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getGrabDeadline() { return grabDeadline; }
    public void setGrabDeadline(LocalDateTime grabDeadline) { this.grabDeadline = grabDeadline; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getServiceStartedAt() { return serviceStartedAt; }
    public void setServiceStartedAt(LocalDateTime serviceStartedAt) { this.serviceStartedAt = serviceStartedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
