package com.eldercare.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 志愿者档案实体，对应 volunteer_db.volunteer_profile 表。
 * 说明：志愿者资料和登录账号分开存储，userId 关联 user-service 的 user_account.id。
 */
@TableName("volunteer_profile")
public class VolunteerProfile {
    /** 主键 ID，使用 MySQL 自增策略。 */
    @TableId(type = IdType.AUTO)
    public Long id;

    /** 社区 ID，用于保证志愿者只能服务自己所在社区的订单。 */
    public Long communityId;

    /** 用户账号 ID，对应 user_db.user_account.id。 */
    public Long userId;

    /** 志愿者展示姓名，第一版直接存储在资料表中。 */
    public String volunteerName;

    /** 志愿者联系电话，用于后续通知或人工联系。 */
    public String volunteerPhone;

    /** 技能标签，第一版用逗号分隔字符串保存，例如：陪诊,保洁。 */
    public String skillTags;

    /** 服务半径，单位米，用于后续按距离筛选志愿者。 */
    public Integer serviceRadiusMeter;

    /** 资料状态：1 正常，0 禁用。 */
    public Integer profileStatus;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    public Integer deleted;
}
