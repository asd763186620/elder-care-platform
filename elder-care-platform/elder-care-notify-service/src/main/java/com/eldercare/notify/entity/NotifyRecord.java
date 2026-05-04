package com.eldercare.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 通知记录实体，对应 notify_db.notify_record 表。
 * 说明：MQ 消费成功后写入该表，既能追踪通知结果，也能通过 mqMessageId 做消费幂等。
 */
@TableName("notify_record")
public class NotifyRecord {
    /** 主键 ID，使用 MySQL 自增策略。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 社区 ID，通知记录同样按社区隔离。 */
    private Long communityId;

    /** 业务类型，第一版主要是 ORDER。 */
    private String businessType;

    /** 业务主键 ID，例如订单 ID。 */
    private Long businessId;

    /** 接收通知的用户 ID。 */
    private Long receiverUserId;

    /** 接收人手机号，第一版可为空，后续对接短信时使用。 */
    private String receiverPhone;

    /** 通知渠道，例如 MOCK、SMS、WECHAT。第一版使用 MOCK 模拟发送。 */
    private String notifyChannel;

    /** 通知标题。 */
    private String notifyTitle;

    /** 通知正文内容。 */
    private String notifyContent;

    /** 已读状态：0未读，1已读。 */
    private Integer readStatus;

    /** 通知状态：1 成功，0 失败。 */
    private Integer notifyStatus;

    /** 重试次数，消费者失败重试时可递增记录。 */
    private Integer retryCount;

    /** 失败原因，通知失败时填写。 */
    private String failReason;

    /** MQ 消息唯一 ID，用于防止重复消费导致重复通知。 */
    private String mqMessageId;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    @TableLogic
    private Integer deleted;

    /**
     * 创建一条模拟发送成功的订单通知记录。
     *
     * @param communityId 社区 ID。
     * @param businessType 业务类型。
     * @param businessId 业务主键 ID。
     * @param receiverUserId 接收通知的用户 ID。
     * @param notifyContent 通知正文。
     * @param mqMessageId MQ 消息唯一 ID。
     * @return 初始化后的通知记录。
     */
    public static NotifyRecord orderSuccess(Long communityId, String businessType, Long businessId, Long receiverUserId, String notifyContent, String mqMessageId) {
        // 构造通知记录。
        NotifyRecord record = new NotifyRecord();
        // 写入社区 ID。
        record.communityId = communityId;
        // 写入业务类型，这里直接使用订单事件类型。
        record.businessType = businessType;
        // 写入业务 ID，也就是订单 ID。
        record.businessId = businessId;
        // 第一版先通知老人用户。
        record.receiverUserId = receiverUserId;
        // 第一版模拟站内通知。
        record.notifyChannel = "IN_APP";
        // 写入通知标题。
        record.notifyTitle = "养老服务预约通知";
        // 写入通知内容。
        record.notifyContent = notifyContent;
        // 第一版模拟发送成功，直接置为 2。
        record.notifyStatus = 2;
        // 新消息默认未读。
        record.readStatus = 0;
        // 初始重试次数为 0。
        record.retryCount = 0;
        // 记录 MQ 消息 ID，后续消费幂等依赖该字段唯一索引。
        record.mqMessageId = mqMessageId;
        // 设置未删除。
        record.deleted = 0;
        // 返回初始化后的通知记录。
        return record;
    }

    public Long getId() {
        // 返回主键 ID。
        return id;
    }

    public void setId(Long id) {
        // MyBatis-Plus 回填或反射设置主键时使用。
        this.id = id;
    }

    public Long getCommunityId() {
        // 返回社区 ID。
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        // MyBatis-Plus 反射设置社区 ID 时使用。
        this.communityId = communityId;
    }

    public String getBusinessType() {
        // 返回业务类型。
        return businessType;
    }

    public void setBusinessType(String businessType) {
        // MyBatis-Plus 反射设置业务类型时使用。
        this.businessType = businessType;
    }

    public Long getBusinessId() {
        // 返回业务主键 ID。
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        // MyBatis-Plus 反射设置业务主键 ID 时使用。
        this.businessId = businessId;
    }

    public Long getReceiverUserId() {
        // 返回接收用户 ID。
        return receiverUserId;
    }

    public void setReceiverUserId(Long receiverUserId) {
        // MyBatis-Plus 反射设置接收用户 ID 时使用。
        this.receiverUserId = receiverUserId;
    }

    public String getReceiverPhone() {
        // 返回接收人手机号。
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        // MyBatis-Plus 反射设置接收人手机号时使用。
        this.receiverPhone = receiverPhone;
    }

    public String getNotifyChannel() {
        // 返回通知渠道。
        return notifyChannel;
    }

    public void setNotifyChannel(String notifyChannel) {
        // MyBatis-Plus 反射设置通知渠道时使用。
        this.notifyChannel = notifyChannel;
    }

    public String getNotifyTitle() {
        // 返回通知标题。
        return notifyTitle;
    }

    public void setNotifyTitle(String notifyTitle) {
        // MyBatis-Plus 反射设置通知标题时使用。
        this.notifyTitle = notifyTitle;
    }

    public String getNotifyContent() {
        // 返回通知内容。
        return notifyContent;
    }

    public void setNotifyContent(String notifyContent) {
        // MyBatis-Plus 反射设置通知内容时使用。
        this.notifyContent = notifyContent;
    }

    public Integer getNotifyStatus() {
        // 返回通知状态。
        return notifyStatus;
    }

    public void setNotifyStatus(Integer notifyStatus) {
        // MyBatis-Plus 反射设置通知状态时使用。
        this.notifyStatus = notifyStatus;
    }

    public Integer getReadStatus() {
        // 返回已读状态。
        return readStatus;
    }

    public void setReadStatus(Integer readStatus) {
        // MyBatis-Plus 反射设置已读状态时使用。
        this.readStatus = readStatus;
    }

    public Integer getRetryCount() {
        // 返回重试次数。
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        // MyBatis-Plus 反射设置重试次数时使用。
        this.retryCount = retryCount;
    }

    public String getFailReason() {
        // 返回失败原因。
        return failReason;
    }

    public void setFailReason(String failReason) {
        // MyBatis-Plus 反射设置失败原因时使用。
        this.failReason = failReason;
    }

    public String getMqMessageId() {
        // 返回 MQ 消息唯一 ID。
        return mqMessageId;
    }

    public void setMqMessageId(String mqMessageId) {
        // MyBatis-Plus 反射设置 MQ 消息唯一 ID 时使用。
        this.mqMessageId = mqMessageId;
    }

    public LocalDateTime getCreatedAt() {
        // 返回创建时间。
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        // MyBatis-Plus 反射设置创建时间时使用。
        this.createdAt = createdAt;
    }

    public Integer getDeleted() {
        // 返回逻辑删除标识。
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        // MyBatis-Plus 反射设置逻辑删除标识时使用。
        this.deleted = deleted;
    }
}
