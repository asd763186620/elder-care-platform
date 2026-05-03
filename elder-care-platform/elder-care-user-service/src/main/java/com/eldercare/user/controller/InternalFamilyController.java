package com.eldercare.user.controller;

import com.eldercare.common.response.Result;
import com.eldercare.user.service.UserAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 亲情号内部接口控制器。
 */
@RestController
@RequestMapping("/internal/family")
public class InternalFamilyController {

    /**
     * 用户业务服务。
     */
    private final UserAppService userAppService;

    /**
     * 构造控制器。
     *
     * @param userAppService 用户业务服务。
     */
    public InternalFamilyController(UserAppService userAppService) {
        // 保存用户业务服务。
        this.userAppService = userAppService;
    }

    /**
     * 校验亲情号是否可以操作老人。
     *
     * @param communityId  社区 ID。
     * @param familyUserId 亲情号用户 ID。
     * @param elderUserId  老人用户 ID。
     * @return true 表示可以操作。
     */
    @GetMapping("/check-bind")
    public Result<Boolean> checkBind(@RequestParam Long communityId,
                                     @RequestParam Long familyUserId,
                                     @RequestParam Long elderUserId) {
        // 调用业务服务校验绑定关系。
        return Result.success(userAppService.checkFamilyBind(communityId, familyUserId, elderUserId));
    }
}
