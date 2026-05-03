package com.eldercare.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 志愿者时间锁实体，对应 volunteer_db.volunteer_time_lock 表。
 * 说明：该表是防止志愿者同一时间段被多个订单占用的核心表。
 */
@TableName("volunteer_time_lock")
public class VolunteerTimeLock {
    /** 主键 ID，使用 MySQL 自增策略。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 社区 ID，用于保证时间锁不会跨社区误占用。 */
    private Long communityId;

    /** 被锁定时间的志愿者用户 ID。 */
    private Long volunteerUserId;

    /** 占用该时间段的订单 ID。 */
    private Long orderId;

    /** 锁定日期，冗余字段便于按天清理和查询。 */
    private LocalDate lockDate;

    /** 锁定开始时间。 */
    private LocalDateTime startTime;

    /** 锁定结束时间。 */
    private LocalDateTime endTime;

    /** 时间段唯一键，通常由 startTime/endTime 组合生成。 */
    private String timeSlotKey;

    /** 锁状态：1 生效，0 已释放。唯一索引只约束生效锁。 */
    private Integer lockStatus;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    @TableLogic
    private Integer deleted;

    /**
     * 创建一条生效的志愿者时间锁。
     *
     * @param communityId 社区 ID。
     * @param volunteerUserId 志愿者用户 ID。
     * @param orderId 订单 ID。
     * @param startTime 锁定开始时间。
     * @param endTime 锁定结束时间。
     * @param timeSlotKey 时间槽唯一键。
     * @param lockedStatus 已锁定状态值。
     * @return 初始化后的时间锁实体。
     */
    public static VolunteerTimeLock locked(Long communityId, Long volunteerUserId, Long orderId, LocalDateTime startTime, LocalDateTime endTime, String timeSlotKey, Integer lockedStatus) {
        // 构造数据库时间锁记录。
        VolunteerTimeLock row = new VolunteerTimeLock();
        // 写入社区 ID。
        row.communityId = communityId;
        // 写入志愿者用户 ID。
        row.volunteerUserId = volunteerUserId;
        // 写入占用该时间段的订单 ID。
        row.orderId = orderId;
        // 写入锁定日期。
        row.lockDate = startTime.toLocalDate();
        // 写入锁定开始时间。
        row.startTime = startTime;
        // 写入锁定结束时间。
        row.endTime = endTime;
        // 写入固定时间槽 key，配合 MySQL 唯一索引兜底。
        row.timeSlotKey = timeSlotKey;
        // 设置已锁定状态。
        row.lockStatus = lockedStatus;
        // 设置未删除。
        row.deleted = 0;
        // 返回初始化后的时间锁。
        return row;
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

    public Long getOrderId() {
        // 返回订单 ID。
        return orderId;
    }

    public void setOrderId(Long orderId) {
        // MyBatis-Plus 反射设置订单 ID 时使用。
        this.orderId = orderId;
    }

    public LocalDate getLockDate() {
        // 返回锁定日期。
        return lockDate;
    }

    public void setLockDate(LocalDate lockDate) {
        // MyBatis-Plus 反射设置锁定日期时使用。
        this.lockDate = lockDate;
    }

    public LocalDateTime getStartTime() {
        // 返回锁定开始时间。
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        // MyBatis-Plus 反射设置锁定开始时间时使用。
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        // 返回锁定结束时间。
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        // MyBatis-Plus 反射设置锁定结束时间时使用。
        this.endTime = endTime;
    }

    public String getTimeSlotKey() {
        // 返回时间槽唯一键。
        return timeSlotKey;
    }

    public void setTimeSlotKey(String timeSlotKey) {
        // MyBatis-Plus 反射设置时间槽唯一键时使用。
        this.timeSlotKey = timeSlotKey;
    }

    public Integer getLockStatus() {
        // 返回时间锁状态。
        return lockStatus;
    }

    public void setLockStatus(Integer lockStatus) {
        // MyBatis-Plus 反射设置时间锁状态时使用。
        this.lockStatus = lockStatus;
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
