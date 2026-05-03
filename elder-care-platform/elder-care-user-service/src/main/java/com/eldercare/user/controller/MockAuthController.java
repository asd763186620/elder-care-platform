package com.eldercare.user.controller;

import com.eldercare.api.dto.MockLoginDTO;
import com.eldercare.api.vo.LoginVO;
import com.eldercare.common.response.Result;
import com.eldercare.user.service.UserAppService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟登录控制器。
 */
@RestController
@RequestMapping("/auth")
public class MockAuthController {

    /**
     * 用户业务服务。
     */
    private final UserAppService userAppService;

    /**
     * 构造控制器。
     *
     * @param userAppService 用户业务服务。
     */
    public MockAuthController(UserAppService userAppService) {
        // 保存用户业务服务。
        this.userAppService = userAppService;
    }

    /**
     * 第一版模拟登录接口。
     *
     * @param loginDTO 模拟登录请求。
     * @return JWT 登录结果。
     */
    @PostMapping("/mock-login")
    public Result<LoginVO> mockLogin(@Valid @RequestBody MockLoginDTO loginDTO) {
        // 调用业务服务生成 Token。
        return Result.success(userAppService.mockLogin(loginDTO));
    }
}
