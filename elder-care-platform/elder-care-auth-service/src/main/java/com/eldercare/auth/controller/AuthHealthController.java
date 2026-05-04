package com.eldercare.auth.controller;

import com.eldercare.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证服务健康检查。
 */
@RestController
public class AuthHealthController {
    /**
     * 简单健康检查。
     */
    @GetMapping("/auth/health")
    public Result<String> health() {
        // 返回认证服务运行状态。
        return Result.success("auth-service ok");
    }
}
