package com.eldercare.api.client;

import com.eldercare.api.client.fallback.CommunityFeignFallbackFactory;
import com.eldercare.common.constant.ServiceNames;
import com.eldercare.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 社区服务 Feign 接口，供订单服务校验服务项目归属。
 */
@FeignClient(name = ServiceNames.COMMUNITY_SERVICE, contextId = "communityFeignClient", fallbackFactory = CommunityFeignFallbackFactory.class)
public interface CommunityFeignClient {

    /**
     * 校验服务项目是否属于指定社区。
     *
     * @param communityId   社区 ID。
     * @param serviceItemId 服务项目 ID。
     * @return true 表示服务项目存在且属于该社区。
     */
    @GetMapping("/internal/service-items/check")
    Result<Boolean> checkServiceItem(@RequestParam("communityId") Long communityId,
                                     @RequestParam("serviceItemId") Long serviceItemId);
}
