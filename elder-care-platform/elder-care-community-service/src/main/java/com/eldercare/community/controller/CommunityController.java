package com.eldercare.community.controller;

import com.eldercare.api.vo.CommunityVO;
import com.eldercare.common.response.Result;
import com.eldercare.community.service.CommunityAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社区接口控制器。
 */
@RestController
@RequestMapping("/communities")
public class CommunityController {

    /**
     * 社区业务服务。
     */
    private final CommunityAppService communityAppService;

    /**
     * 构造控制器。
     *
     * @param communityAppService 社区业务服务。
     */
    public CommunityController(CommunityAppService communityAppService) {
        // 保存社区业务服务。
        this.communityAppService = communityAppService;
    }

    /**
     * 查询当前用户所属社区。
     *
     * @return 当前社区信息。
     */
    @GetMapping("/current")
    public Result<CommunityVO> current() {
        // 调用业务服务查询当前社区。
        return Result.success(communityAppService.getCurrentCommunity());
    }
}
