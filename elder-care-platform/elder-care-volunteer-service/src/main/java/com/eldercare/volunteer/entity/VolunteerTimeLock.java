package com.eldercare.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
    public Long id;

    /** 社区 ID，用于保证时间锁不会跨社区误占用。 */
    public Long communityId;

    /** 被锁定时间的志愿者用户 ID。 */
    public Long volunteerUserId;

    /** 占用该时间段的订单 ID。 */
    public Long orderId;

    /** 锁定日期，冗余字段便于按天清理和查询。 */
    public LocalDate lockDate;

    /** 锁定开始时间。 */
    public LocalDateTime startTime;

    /** 锁定结束时间。 */
    public LocalDateTime endTime;

    /** 时间段唯一键，通常由 startTime/endTime 组合生成。 */
    public String timeSlotKey;

    /** 锁状态：1 生效，0 已释放。唯一索引只约束生效锁。 */
    public Integer lockStatus;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    public Integer deleted;
}
