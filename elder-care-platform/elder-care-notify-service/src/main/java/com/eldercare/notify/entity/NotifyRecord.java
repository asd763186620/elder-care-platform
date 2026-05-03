package com.eldercare.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 通知记录实体，对应 notify_db.notify_record 表。
 * 说明：MQ 消费成功后写入该表，既能追踪通知结果，也能通过 mqMessageId 做消费幂等。
 */
@TableName("notify_record")
public class NotifyRecord {
    /** 主键 ID，使用 MySQL 自增策略。 */
    @TableId(type = IdType.AUTO)
    public Long id;

    /** 社区 ID，通知记录同样按社区隔离。 */
    public Long communityId;

    /** 业务类型，第一版主要是 ORDER。 */
    public String businessType;

    /** 业务主键 ID，例如订单 ID。 */
    public Long businessId;

    /** 接收通知的用户 ID。 */
    public Long receiverUserId;

    /** 接收人手机号，第一版可为空，后续对接短信时使用。 */
    public String receiverPhone;

    /** 通知渠道，例如 MOCK、SMS、WECHAT。第一版使用 MOCK 模拟发送。 */
    public String notifyChannel;

    /** 通知标题。 */
    public String notifyTitle;

    /** 通知正文内容。 */
    public String notifyContent;

    /** 通知状态：1 成功，0 失败。 */
    public Integer notifyStatus;

    /** 重试次数，消费者失败重试时可递增记录。 */
    public Integer retryCount;

    /** 失败原因，通知失败时填写。 */
    public String failReason;

    /** MQ 消息唯一 ID，用于防止重复消费导致重复通知。 */
    public String mqMessageId;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    public Integer deleted;
}
