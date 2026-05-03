package com.eldercare.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.response.Result;
import com.eldercare.user.entity.ElderProfile;
import com.eldercare.user.entity.FamilyElderBind;
import com.eldercare.user.entity.UserAccount;
import com.eldercare.user.mapper.ElderProfileMapper;
import com.eldercare.user.mapper.FamilyElderBindMapper;
import com.eldercare.user.mapper.UserAccountMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台用户、老人和亲情号查询接口。
 */
@RestController
@RequestMapping("/admin/users")
@RequireRole(RoleConstants.ADMIN)
public class UserAdminController {
    /** 用户账号 Mapper。 */
    private final UserAccountMapper userAccountMapper;

    /** 老人档案 Mapper。 */
    private final ElderProfileMapper elderProfileMapper;

    /** 亲情号绑定 Mapper。 */
    private final FamilyElderBindMapper familyElderBindMapper;

    public UserAdminController(UserAccountMapper userAccountMapper, ElderProfileMapper elderProfileMapper, FamilyElderBindMapper familyElderBindMapper) {
        // 保存用户账号 Mapper。
        this.userAccountMapper = userAccountMapper;
        // 保存老人档案 Mapper。
        this.elderProfileMapper = elderProfileMapper;
        // 保存亲情号绑定 Mapper。
        this.familyElderBindMapper = familyElderBindMapper;
    }

    /**
     * 查询当前社区最近用户账号。
     *
     * @return 用户列表。
     */
    @GetMapping
    public Result<List<UserAccount>> users() {
        // 加载管理员上下文。
        UserContext.loadFromCurrentRequest();
        // 读取社区 ID。
        Long communityId = UserContext.requireCommunityId();
        // 查询最近 100 个账号。
        return Result.success(userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getCommunityId, communityId).eq(UserAccount::getDeleted, 0).orderByDesc(UserAccount::getId).last("LIMIT 100")));
    }

    /**
     * 查询当前社区老人档案。
     *
     * @return 老人档案列表。
     */
    @GetMapping("/elders")
    public Result<List<ElderProfile>> elders() {
        // 加载管理员上下文。
        UserContext.loadFromCurrentRequest();
        // 读取社区 ID。
        Long communityId = UserContext.requireCommunityId();
        // 查询老人档案。
        return Result.success(elderProfileMapper.selectList(new LambdaQueryWrapper<ElderProfile>().eq(ElderProfile::getCommunityId, communityId).eq(ElderProfile::getDeleted, 0).orderByDesc(ElderProfile::getId).last("LIMIT 100")));
    }

    /**
     * 查询当前社区亲情号绑定关系。
     *
     * @return 绑定关系列表。
     */
    @GetMapping("/family-binds")
    public Result<List<FamilyElderBind>> familyBinds() {
        // 加载管理员上下文。
        UserContext.loadFromCurrentRequest();
        // 读取社区 ID。
        Long communityId = UserContext.requireCommunityId();
        // 查询绑定关系。
        return Result.success(familyElderBindMapper.selectList(new LambdaQueryWrapper<FamilyElderBind>().eq(FamilyElderBind::getCommunityId, communityId).eq(FamilyElderBind::getDeleted, 0).orderByDesc(FamilyElderBind::getId).last("LIMIT 100")));
    }
}
