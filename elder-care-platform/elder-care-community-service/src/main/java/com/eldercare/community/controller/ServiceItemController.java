package com.eldercare.community.controller;

import com.eldercare.api.vo.ServiceItemVO;
import com.eldercare.common.response.Result;
import com.eldercare.community.service.CommunityAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务项目接口控制器。
 */
@RestController
@RequestMapping("/service-items")
public class ServiceItemController {

    /**
     * 社区业务服务。
     */
    private final CommunityAppService communityAppService;

    /**
     * 构造控制器。
     *
     * @param communityAppService 社区业务服务。
     */
    public ServiceItemController(CommunityAppService communityAppService) {
        // 保存社区业务服务。
        this.communityAppService = communityAppService;
    }

    /**
     * 查询当前社区下的服务项目。
     *
     * @return 服务项目列表。
     */
    @GetMapping
    public Result<List<ServiceItemVO>> list() {
        // 调用业务服务查询服务项目列表。
        return Result.success(communityAppService.listServiceItems());
    }

    /**
     * 查询服务项目详情。
     *
     * @param id 服务项目 ID。
     * @return 服务项目详情。
     */
    @GetMapping("/{id}")
    public Result<ServiceItemVO> detail(@PathVariable Long id) {
        // 调用业务服务查询服务项目详情。
        return Result.success(communityAppService.getServiceItem(id));
    }
}
