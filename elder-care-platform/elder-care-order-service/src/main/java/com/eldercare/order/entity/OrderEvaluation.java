package com.eldercare.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 订单评价实体。
 */
@TableName("order_evaluation")
public class OrderEvaluation {
    /** 主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订单 ID，唯一索引用于防止重复评价。 */
    private Long orderId;
    /** 老人用户 ID。 */
    private Long elderUserId;
    /** 亲情号用户 ID，老人本人评价时为空。 */
    private Long familyUserId;
    /** 志愿者用户 ID。 */
    private Long volunteerUserId;
    /** 社区 ID。 */
    private Long communityId;
    /** 评分。 */
    private Integer score;
    /** 标签。 */
    private String tags;
    /** 评价内容。 */
    private String content;
    /** 是否匿名。 */
    private Boolean anonymous;
    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 创建评价记录。 */
    public static OrderEvaluation of(ServiceOrder order, Long operatorId, Integer score, String tags, String content, Boolean anonymous) {
        // 构造评价实体。
        OrderEvaluation evaluation = new OrderEvaluation();
        // 绑定订单。
        evaluation.orderId = order.getId();
        // 写入被服务老人。
        evaluation.elderUserId = order.getElderUserId();
        // 亲情号评价时记录亲情号 ID，老人本人评价时为空。
        evaluation.familyUserId = operatorId.equals(order.getElderUserId()) ? null : operatorId;
        // 写入志愿者。
        evaluation.volunteerUserId = order.getAssignedVolunteerUserId();
        // 写入社区。
        evaluation.communityId = order.getCommunityId();
        // 写入评分。
        evaluation.score = score;
        // 写入标签。
        evaluation.tags = tags;
        // 写入正文。
        evaluation.content = content;
        // 写入匿名标记。
        evaluation.anonymous = Boolean.TRUE.equals(anonymous);
        // 写入创建时间。
        evaluation.createTime = LocalDateTime.now();
        // 返回评价对象。
        return evaluation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getElderUserId() { return elderUserId; }
    public void setElderUserId(Long elderUserId) { this.elderUserId = elderUserId; }
    public Long getFamilyUserId() { return familyUserId; }
    public void setFamilyUserId(Long familyUserId) { this.familyUserId = familyUserId; }
    public Long getVolunteerUserId() { return volunteerUserId; }
    public void setVolunteerUserId(Long volunteerUserId) { this.volunteerUserId = volunteerUserId; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getAnonymous() { return anonymous; }
    public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
