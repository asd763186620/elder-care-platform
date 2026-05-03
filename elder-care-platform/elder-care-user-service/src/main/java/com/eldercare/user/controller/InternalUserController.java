package com.eldercare.user.controller;

import com.eldercare.common.response.Result;
import com.eldercare.user.service.UserAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户服务内部接口，供其他微服务通过 Feign 调用。
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    /**
     * 用户业务服务。
     */
    private final UserAppService userAppService;

    /**
     * 构造内部接口控制器。
     *
     * @param userAppService 用户业务服务。
     */
    public InternalUserController(UserAppService userAppService) {
        // 保存用户业务服务。
        this.userAppService = userAppService;
    }

    /**
     * 判断用户是否存在。
     *
     * @param userId 用户 ID。
     * @return true 表示存在。
     */
    @GetMapping("/{userId}/exists")
    public Result<Boolean> existsById(@PathVariable Long userId) {
        // 使用数据库中的真实账号判断用户是否存在。
        return Result.success(userAppService.existsById(userId));
    }
}
