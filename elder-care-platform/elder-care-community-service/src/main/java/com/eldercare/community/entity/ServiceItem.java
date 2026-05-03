package com.eldercare.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 服务项目实体，对应 community_db.service_item。
 */
@TableName("service_item")
public class ServiceItem {

    /**
     * 服务项目主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属社区 ID。
     */
    private Long communityId;

    /**
     * 服务项目名称。
     */
    private String itemName;

    /**
     * 服务项目编码。
     */
    private String itemCode;

    /**
     * 服务项目说明。
     */
    private String itemDesc;

    /**
     * 默认服务时长，单位分钟。
     */
    private Integer durationMinutes;

    /**
     * 服务价格，单位分。
     */
    private Integer priceCent;

    /**
     * 项目状态：1启用，2停用。
     */
    private Integer itemStatus;

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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemDesc() {
        return itemDesc;
    }

    public void setItemDesc(String itemDesc) {
        this.itemDesc = itemDesc;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getPriceCent() {
        return priceCent;
    }

    public void setPriceCent(Integer priceCent) {
        this.priceCent = priceCent;
    }

    public Integer getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(Integer itemStatus) {
        this.itemStatus = itemStatus;
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
