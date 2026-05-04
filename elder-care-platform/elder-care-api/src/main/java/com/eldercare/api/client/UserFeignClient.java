package com.eldercare.api.client;

import com.eldercare.common.constant.ServiceNames;
import com.eldercare.common.response.Result;
import com.eldercare.api.client.fallback.UserFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务 Feign 接口，供其他服务远程校验用户信息。
 */
@FeignClient(name = ServiceNames.USER_SERVICE, contextId = "userFeignClient", fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignClient {

    /**
     * 检查用户是否存在。
     *
     * @param userId 用户 ID。
     * @return true 表示存在，false 表示不存在。
     */
    @GetMapping("/internal/users/{userId}/exists")
    Result<Boolean> existsById(@PathVariable("userId") Long userId);

    /**
     * 校验亲情号是否已绑定老人。
     *
     * @param communityId  社区 ID。
     * @param familyUserId 亲情号用户 ID。
     * @param elderUserId  老人用户 ID。
     * @return true 表示已绑定且可代老人操作。
     */
    @GetMapping("/internal/family/check-bind")
    Result<Boolean> checkFamilyBind(@RequestParam("communityId") Long communityId,
                                    @RequestParam("familyUserId") Long familyUserId,
                                    @RequestParam("elderUserId") Long elderUserId);
}
