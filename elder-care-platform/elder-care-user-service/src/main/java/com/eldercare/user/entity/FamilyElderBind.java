package com.eldercare.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 亲情号绑定老人实体，对应 user_db.family_elder_bind。
 */
@TableName("family_elder_bind")
public class FamilyElderBind {

    /**
     * 绑定记录主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属社区 ID。
     */
    private Long communityId;

    /**
     * 亲情号用户 ID。
     */
    private Long familyUserId;

    /**
     * 老人用户 ID。
     */
    private Long elderUserId;

    /**
     * 亲属关系。
     */
    private String relationship;

    /**
     * 绑定状态：1待确认，2已绑定，3已拒绝，4已解绑。
     */
    private Integer bindStatus;

    /**
     * 绑定来源。
     */
    private String bindSource;

    /**
     * 确认时间。
     */
    private LocalDateTime confirmedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除。
     */
    @TableLogic
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public Long getFamilyUserId() {
        return familyUserId;
    }

    public void setFamilyUserId(Long familyUserId) {
        this.familyUserId = familyUserId;
    }

    public Long getElderUserId() {
        return elderUserId;
    }

    public void setElderUserId(Long elderUserId) {
        this.elderUserId = elderUserId;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public Integer getBindStatus() {
        return bindStatus;
    }

    public void setBindStatus(Integer bindStatus) {
        this.bindStatus = bindStatus;
    }

    public String getBindSource() {
        return bindSource;
    }

    public void setBindSource(String bindSource) {
        this.bindSource = bindSource;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
