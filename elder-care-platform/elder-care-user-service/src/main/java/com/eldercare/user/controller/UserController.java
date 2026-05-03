package com.eldercare.user.controller;

import com.eldercare.api.vo.UserInfoVO;
import com.eldercare.common.response.Result;
import com.eldercare.user.service.UserAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户账号控制器。
 */
@RestController
@RequestMapping("/users")
@Tag(name = "用户接口", description = "当前登录用户信息")
public class UserController {

    /**
     * 用户业务服务。
     */
    private final UserAppService userAppService;

    /**
     * 构造控制器。
     *
     * @param userAppService 用户业务服务。
     */
    public UserController(UserAppService userAppService) {
        // 保存用户业务服务。
        this.userAppService = userAppService;
    }

    /**
     * 查询当前登录用户信息。
     *
     * @return 当前用户信息。
     */
    @GetMapping("/me")
    @Operation(summary = "查询当前登录用户", description = "根据网关透传的用户上下文查询账号和角色信息")
    public Result<UserInfoVO> me() {
        // 调用业务服务查询当前用户。
        return Result.success(userAppService.getCurrentUser());
    }
}
