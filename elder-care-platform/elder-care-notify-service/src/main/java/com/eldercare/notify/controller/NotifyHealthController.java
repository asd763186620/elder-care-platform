package com.eldercare.notify.controller;

import com.eldercare.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知服务健康检查控制器。
 */
@RestController
@RequestMapping("/api/notify")
public class NotifyHealthController {

    /**
     * 服务连通性检查。
     *
     * @return 服务名称。
     */
    @GetMapping("/ping")
    public Result<String> ping() {
        // 返回服务名，便于通过网关验证路由是否生效。
        return Result.success("elder-care-notify-service");
    }
}
