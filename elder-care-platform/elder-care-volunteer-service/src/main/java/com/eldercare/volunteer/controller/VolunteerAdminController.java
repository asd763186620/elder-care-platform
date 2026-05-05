package com.eldercare.volunteer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.enums.RoleEnum;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.response.Result;
import com.eldercare.volunteer.entity.VolunteerProfile;
import com.eldercare.volunteer.mapper.VolunteerProfileMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台志愿者审核管理接口。
 */
@RestController
@RequestMapping("/admin/volunteers")
@RequireRole(RoleEnum.ADMIN)
public class VolunteerAdminController {
    /** 志愿者档案 Mapper。 */
    private final VolunteerProfileMapper profileMapper;

    public VolunteerAdminController(VolunteerProfileMapper profileMapper) {
        // 保存志愿者档案 Mapper。
        this.profileMapper = profileMapper;
    }

    /**
     * 查询当前社区志愿者档案。
     *
     * @return 志愿者档案列表。
     */
    @GetMapping
    public Result<List<VolunteerProfile>> list() {
        // 加载管理员上下文。
        UserContext.loadFromCurrentRequest();
        // 读取社区 ID。
        Long communityId = UserContext.requireCommunityId();
        // 查询最近 100 个志愿者档案。
        return Result.success(profileMapper.selectList(new LambdaQueryWrapper<VolunteerProfile>().eq(VolunteerProfile::getCommunityId, communityId).eq(VolunteerProfile::getDeleted, 0).orderByDesc(VolunteerProfile::getId).last("LIMIT 100")));
    }

    /**
     * 审核通过志愿者。
     *
     * @param userId 志愿者用户 ID。
     * @return 空结果。
     */
    @PostMapping("/{userId}/approve")
    public Result<Void> approve(@PathVariable Long userId) {
        // 加载管理员上下文。
        UserContext.loadFromCurrentRequest();
        // 读取社区 ID。
        Long communityId = UserContext.requireCommunityId();
        // 将志愿者档案置为正常状态。
        profileMapper.update(null, new UpdateWrapper<VolunteerProfile>()
                .set("profile_status", 2)
                .set("audit_time", LocalDateTime.now())
                .eq("community_id", communityId)
                .eq("user_id", userId)
                .eq("deleted", 0));
        // 返回成功。
        return Result.success();
    }
}
