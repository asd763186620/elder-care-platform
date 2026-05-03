package com.eldercare.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
    public Long id;

    /** 社区 ID，用于限制志愿者只能出现在本社区的可用列表中。 */
    public Long communityId;

    /** 志愿者用户 ID，对应 user_db.user_account.id。 */
    public Long volunteerUserId;

    /** 服务项目 ID，用于表示该时间段志愿者可以接哪类服务。 */
    public Long serviceItemId;

    /** 可服务日期，冗余日期字段便于按天查询和建索引。 */
    public LocalDate availableDate;

    /** 可服务开始时间，必须早于 endTime。 */
    public LocalDateTime startTime;

    /** 可服务结束时间，必须晚于 startTime。 */
    public LocalDateTime endTime;

    /** 可用状态：1 可用，0 停用。 */
    public Integer availableStatus;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    public Integer deleted;
}
