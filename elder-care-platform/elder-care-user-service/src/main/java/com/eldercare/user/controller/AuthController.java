package com.eldercare.user.controller;

import com.eldercare.api.dto.UserLoginDTO;
import com.eldercare.api.vo.LoginVO;
import com.eldercare.common.context.LoginUser;
import com.eldercare.common.jwt.JwtUtil;
import com.eldercare.common.response.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证控制器，第一版先用手机号和角色模拟登录。
 */
@RestController
@RequestMapping("/api/user/auth")
public class AuthController {

    /**
     * JWT 工具类。
     */
    private final JwtUtil jwtUtil;

    /**
     * 构造认证控制器。
     *
     * @param jwtUtil JWT 工具类。
     */
    public AuthController(JwtUtil jwtUtil) {
        // 保存 JWT 工具类，用于签发登录 Token。
        this.jwtUtil = jwtUtil;
    }

    /**
     * 登录接口。
     *
     * @param loginDTO 登录请求。
     * @return 登录响应。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        // 第一版用手机号哈希生成稳定的演示用户 ID。
        long userId = Math.abs((long) loginDTO.phone().hashCode());
        // 第一版没有社区选择页时，默认使用演示社区 1。
        long communityId = loginDTO.communityId() == null ? 1L : loginDTO.communityId();
        // 组装登录用户信息。
        LoginUser loginUser = new LoginUser(userId, communityId, java.util.List.of(loginDTO.role().name()), loginDTO.role(), loginDTO.phone());
        // 生成 JWT Token。
        String token = jwtUtil.generateToken(loginUser);
        // 返回用户 ID、角色和 Token。
        return Result.success(new LoginVO(userId, loginDTO.role(), token));
    }
}
