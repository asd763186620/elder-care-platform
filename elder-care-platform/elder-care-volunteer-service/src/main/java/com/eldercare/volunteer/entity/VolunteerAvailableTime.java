package com.eldercare.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 志愿者可服务时间实体，对应 volunteer_db.volunteer_available_time 表。
 * 说明：该表表达“志愿者愿意服务的时间段”，真正占用时间时还需要写入 volunteer_time_lock。
 */
@TableName("volunteer_available_time")
public class VolunteerAvailableTime {
    /** 主键 ID，使用 MySQL 自增策略。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 社区 ID，用于限制志愿者只能出现在本社区的可用列表中。 */
    private Long communityId;

    /** 志愿者用户 ID，对应 user_db.user_account.id。 */
    private Long volunteerUserId;

    /** 服务项目 ID，用于表示该时间段志愿者可以接哪类服务。 */
    private Long serviceItemId;

    /** 可服务日期，冗余日期字段便于按天查询和建索引。 */
    private LocalDate availableDate;

    /** 可服务开始时间，必须早于 endTime。 */
    private LocalDateTime startTime;

    /** 可服务结束时间，必须晚于 startTime。 */
    private LocalDateTime endTime;

    /** 可用状态：1 可用，0 停用。 */
    private Integer availableStatus;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    @TableLogic
    private Integer deleted;

    /**
     * 创建一条志愿者可服务时间。
     *
     * @param communityId 志愿者所属社区 ID。
     * @param volunteerUserId 志愿者用户 ID。
     * @param serviceItemId 服务项目 ID，允许为空表示不限项目。
     * @param startTime 可服务开始时间。
     * @param endTime 可服务结束时间。
     * @param availableStatus 可用状态。
     * @return 初始化后的可服务时间实体。
     */
    public static VolunteerAvailableTime create(Long communityId, Long volunteerUserId, Long serviceItemId, LocalDateTime startTime, LocalDateTime endTime, Integer availableStatus) {
        // 构造可服务时间实体。
        VolunteerAvailableTime time = new VolunteerAvailableTime();
        // 写入社区 ID，确保志愿者只能服务本社区订单。
        time.communityId = communityId;
        // 写入志愿者用户 ID。
        time.volunteerUserId = volunteerUserId;
        // 写入可服务项目 ID，允许为空表示不限项目。
        time.serviceItemId = serviceItemId;
        // 写入日期字段，便于按天查询。
        time.availableDate = startTime.toLocalDate();
        // 写入可服务开始时间。
        time.startTime = startTime;
        // 写入可服务结束时间。
        time.endTime = endTime;
        // 设置可用状态。
        time.availableStatus = availableStatus;
        // 设置未删除。
        time.deleted = 0;
        // 返回初始化后的实体。
        return time;
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

    public Long getVolunteerUserId() {
        // 返回志愿者用户 ID。
        return volunteerUserId;
    }

    public void setVolunteerUserId(Long volunteerUserId) {
        // MyBatis-Plus 反射设置志愿者用户 ID 时使用。
        this.volunteerUserId = volunteerUserId;
    }

    public Long getServiceItemId() {
        // 返回服务项目 ID。
        return serviceItemId;
    }

    public void setServiceItemId(Long serviceItemId) {
        // MyBatis-Plus 反射设置服务项目 ID 时使用。
        this.serviceItemId = serviceItemId;
    }

    public LocalDate getAvailableDate() {
        // 返回可服务日期。
        return availableDate;
    }

    public void setAvailableDate(LocalDate availableDate) {
        // MyBatis-Plus 反射设置可服务日期时使用。
        this.availableDate = availableDate;
    }

    public LocalDateTime getStartTime() {
        // 返回可服务开始时间。
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        // MyBatis-Plus 反射设置可服务开始时间时使用。
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        // 返回可服务结束时间。
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        // MyBatis-Plus 反射设置可服务结束时间时使用。
        this.endTime = endTime;
    }

    public Integer getAvailableStatus() {
        // 返回可用状态。
        return availableStatus;
    }

    public void setAvailableStatus(Integer availableStatus) {
        // MyBatis-Plus 反射设置可用状态时使用。
        this.availableStatus = availableStatus;
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
