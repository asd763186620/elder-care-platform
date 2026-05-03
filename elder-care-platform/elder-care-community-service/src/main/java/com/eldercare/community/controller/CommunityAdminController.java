package com.eldercare.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.response.Result;
import com.eldercare.community.entity.Community;
import com.eldercare.community.entity.ServiceItem;
import com.eldercare.community.mapper.CommunityMapper;
import com.eldercare.community.mapper.ServiceItemMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台社区与服务项目管理接口。
 */
@RestController
@RequestMapping("/admin/community")
@RequireRole(RoleConstants.ADMIN)
public class CommunityAdminController {
    /** 社区 Mapper。 */
    private final CommunityMapper communityMapper;

    /** 服务项目 Mapper。 */
    private final ServiceItemMapper serviceItemMapper;

    public CommunityAdminController(CommunityMapper communityMapper, ServiceItemMapper serviceItemMapper) {
        // 保存社区 Mapper。
        this.communityMapper = communityMapper;
        // 保存服务项目 Mapper。
        this.serviceItemMapper = serviceItemMapper;
    }

    /**
     * 查询当前管理员所属社区。
     *
     * @return 社区记录。
     */
    @GetMapping
    public Result<Community> currentCommunity() {
        // 加载用户上下文。
        UserContext.loadFromCurrentRequest();
        // 读取社区 ID。
        Long communityId = UserContext.requireCommunityId();
        // 查询当前社区。
        return Result.success(communityMapper.selectOne(new LambdaQueryWrapper<Community>().eq(Community::getCommunityId, communityId).eq(Community::getDeleted, 0).last("LIMIT 1")));
    }

    /**
     * 后台查询服务项目，包含已下架项目。
     *
     * @return 服务项目列表。
     */
    @GetMapping("/service-items")
    public Result<List<ServiceItem>> serviceItems() {
        // 加载用户上下文。
        UserContext.loadFromCurrentRequest();
        // 读取社区 ID。
        Long communityId = UserContext.requireCommunityId();
        // 查询当前社区服务项目。
        return Result.success(serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>().eq(ServiceItem::getCommunityId, communityId).eq(ServiceItem::getDeleted, 0).orderByDesc(ServiceItem::getId)));
    }

    /**
     * 上架或下架服务项目。
     *
     * @param id     服务项目 ID。
     * @param status 状态：1 上架，2 下架。
     * @return 空结果。
     */
    @PostMapping("/service-items/{id}/status/{status}")
    public Result<Void> updateServiceItemStatus(@PathVariable Long id, @PathVariable Integer status) {
        // 加载用户上下文。
        UserContext.loadFromCurrentRequest();
        // 读取社区 ID。
        Long communityId = UserContext.requireCommunityId();
        // 按社区和项目 ID 更新状态，防止跨社区操作。
        serviceItemMapper.update(null, new LambdaUpdateWrapper<ServiceItem>().set(ServiceItem::getItemStatus, status).eq(ServiceItem::getCommunityId, communityId).eq(ServiceItem::getId, id).eq(ServiceItem::getDeleted, 0));
        // 返回成功。
        return Result.success();
    }
}
