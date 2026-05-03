package com.eldercare.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 老人档案实体，对应 user_db.elder_profile。
 */
@TableName("elder_profile")
public class ElderProfile {

    /**
     * 老人档案主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属社区 ID。
     */
    private Long communityId;

    /**
     * 老人用户 ID。
     */
    private Long userId;

    /**
     * 老人姓名。
     */
    private String elderName;

    /**
     * 老人联系电话。
     */
    private String elderPhone;

    /**
     * 年龄。
     */
    private Integer age;

    /**
     * 详细住址。
     */
    private String address;

    /**
     * 健康情况备注。
     */
    private String healthNote;

    /**
     * 紧急联系人姓名。
     */
    private String emergencyContactName;

    /**
     * 紧急联系人手机号。
     */
    private String emergencyContactPhone;

    /**
     * 档案状态。
     */
    private Integer profileStatus;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getElderName() {
        return elderName;
    }

    public void setElderName(String elderName) {
        this.elderName = elderName;
    }

    public String getElderPhone() {
        return elderPhone;
    }

    public void setElderPhone(String elderPhone) {
        this.elderPhone = elderPhone;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getHealthNote() {
        return healthNote;
    }

    public void setHealthNote(String healthNote) {
        this.healthNote = healthNote;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public Integer getProfileStatus() {
        return profileStatus;
    }

    public void setProfileStatus(Integer profileStatus) {
        this.profileStatus = profileStatus;
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
