package com.eldercare.community.controller;

import com.eldercare.common.response.Result;
import com.eldercare.community.service.CommunityAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务项目内部接口控制器。
 */
@RestController
@RequestMapping("/internal/service-items")
public class InternalServiceItemController {

    /**
     * 社区业务服务。
     */
    private final CommunityAppService communityAppService;

    /**
     * 构造控制器。
     *
     * @param communityAppService 社区业务服务。
     */
    public InternalServiceItemController(CommunityAppService communityAppService) {
        // 保存社区业务服务。
        this.communityAppService = communityAppService;
    }

    /**
     * 校验服务项目是否属于当前社区。
     *
     * @param communityId   社区 ID。
     * @param serviceItemId 服务项目 ID。
     * @return true 表示服务项目属于该社区。
     */
    @GetMapping("/check")
    public Result<Boolean> check(@RequestParam Long communityId, @RequestParam Long serviceItemId) {
        // 调用业务服务校验服务项目归属。
        return Result.success(communityAppService.checkServiceItem(communityId, serviceItemId));
    }
}
