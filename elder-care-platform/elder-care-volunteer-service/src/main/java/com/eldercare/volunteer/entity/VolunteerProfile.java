package com.eldercare.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 志愿者档案实体，对应 volunteer_db.volunteer_profile 表。
 * 说明：志愿者资料和登录账号分开存储，userId 关联 user-service 的 user_account.id。
 */
@TableName("volunteer_profile")
public class VolunteerProfile {
    /** 主键 ID，使用 MySQL 自增策略。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 社区 ID，用于保证志愿者只能服务自己所在社区的订单。 */
    private Long communityId;

    /** 用户账号 ID，对应 user_db.user_account.id。 */
    private Long userId;

    /** 志愿者展示姓名，第一版直接存储在资料表中。 */
    private String volunteerName;

    /** 志愿者联系电话，用于后续通知或人工联系。 */
    private String volunteerPhone;

    /** 技能标签，第一版用逗号分隔字符串保存，例如：陪诊,保洁。 */
    private String skillTags;

    /** 服务半径，单位米，用于后续按距离筛选志愿者。 */
    private Integer serviceRadiusMeter;

    /** 资料状态：1 正常，0 禁用。 */
    private Integer profileStatus;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    @TableLogic
    private Integer deleted;

    /**
     * 创建一个属于当前社区和当前用户的志愿者档案。
     *
     * @param communityId 当前用户所属社区 ID。
     * @param userId 当前登录用户 ID。
     * @return 初始化后的志愿者档案。
     */
    public static VolunteerProfile create(Long communityId, Long userId) {
        // 构造志愿者档案实体。
        VolunteerProfile profile = new VolunteerProfile();
        // 写入社区 ID，后续所有订单匹配都靠这个字段隔离。
        profile.communityId = communityId;
        // 写入当前登录用户 ID。
        profile.userId = userId;
        // 新档案默认未删除。
        profile.deleted = 0;
        // 返回初始化后的档案。
        return profile;
    }

    /**
     * 使用前端提交的资料刷新志愿者档案。
     *
     * @param volunteerName 志愿者姓名。
     * @param volunteerPhone 志愿者手机号。
     * @param skillTags 技能标签。
     * @param serviceRadiusMeter 服务半径。
     * @param normalStatus 正常档案状态。
     */
    public void updateProfile(String volunteerName, String volunteerPhone, String skillTags, Integer serviceRadiusMeter, Integer normalStatus) {
        // 更新志愿者姓名。
        this.volunteerName = volunteerName;
        // 更新志愿者手机号。
        this.volunteerPhone = volunteerPhone;
        // 更新技能标签，第一版用逗号分隔字符串。
        this.skillTags = skillTags;
        // 更新服务半径。
        this.serviceRadiusMeter = serviceRadiusMeter;
        // 第一版提交资料后直接置为正常状态。
        this.profileStatus = normalStatus;
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

    public Long getUserId() {
        // 返回用户 ID。
        return userId;
    }

    public void setUserId(Long userId) {
        // MyBatis-Plus 反射设置用户 ID 时使用。
        this.userId = userId;
    }

    public String getVolunteerName() {
        // 返回志愿者姓名。
        return volunteerName;
    }

    public void setVolunteerName(String volunteerName) {
        // MyBatis-Plus 反射设置志愿者姓名时使用。
        this.volunteerName = volunteerName;
    }

    public String getVolunteerPhone() {
        // 返回志愿者手机号。
        return volunteerPhone;
    }

    public void setVolunteerPhone(String volunteerPhone) {
        // MyBatis-Plus 反射设置志愿者手机号时使用。
        this.volunteerPhone = volunteerPhone;
    }

    public String getSkillTags() {
        // 返回技能标签。
        return skillTags;
    }

    public void setSkillTags(String skillTags) {
        // MyBatis-Plus 反射设置技能标签时使用。
        this.skillTags = skillTags;
    }

    public Integer getServiceRadiusMeter() {
        // 返回服务半径。
        return serviceRadiusMeter;
    }

    public void setServiceRadiusMeter(Integer serviceRadiusMeter) {
        // MyBatis-Plus 反射设置服务半径时使用。
        this.serviceRadiusMeter = serviceRadiusMeter;
    }

    public Integer getProfileStatus() {
        // 返回档案状态。
        return profileStatus;
    }

    public void setProfileStatus(Integer profileStatus) {
        // MyBatis-Plus 反射设置档案状态时使用。
        this.profileStatus = profileStatus;
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
