package com.eldercare.user.controller;

import com.eldercare.api.dto.MockLoginDTO;
import com.eldercare.api.dto.UserLoginDTO;
import com.eldercare.api.vo.LoginVO;
import com.eldercare.common.response.Result;
import com.eldercare.user.service.UserAppService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证控制器，兼容早期手机号登录入口。
 */
@RestController
@RequestMapping("/api/user/auth")
public class AuthController {

    /**
     * 用户业务服务。
     */
    private final UserAppService userAppService;

    /**
     * 构造认证控制器。
     *
     * @param userAppService 用户业务服务。
     */
    public AuthController(UserAppService userAppService) {
        // 保存用户业务服务，保证登录逻辑统一走账号、角色和刷新令牌体系。
        this.userAppService = userAppService;
    }

    /**
     * 登录接口。
     *
     * @param loginDTO 登录请求。
     * @return 登录响应。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        // 兼容旧入口：手机号登录复用 mockLogin 的真实落库、角色和 refresh token 逻辑。
        return Result.success(userAppService.mockLogin(new MockLoginDTO(null, loginDTO.phone(), loginDTO.communityId(), java.util.List.of(loginDTO.role().name()))));
    }
}
