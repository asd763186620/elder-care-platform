package com.eldercare.api.client.fallback;

import com.eldercare.api.client.UserFeignClient;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.common.response.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * user-service Feign 降级工厂。
 * 说明：远程服务不可用时返回统一失败 Result，调用方再转成明确业务异常。
 */
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {
    /** 日志对象。 */
    private static final Logger log = LoggerFactory.getLogger(UserFeignFallbackFactory.class);

    @Override
    public UserFeignClient create(Throwable cause) {
        // 打印远程调用异常，便于排查 Nacos、网络或服务异常。
        log.error("user-service feign fallback triggered", cause);
        // 返回降级实现。
        return new UserFeignClient() {
            @Override
            public Result<Boolean> existsById(Long userId) {
                // 返回明确失败，不让 order-service 看到裸 500。
                return Result.fail(ErrorCode.BIZ_ERROR, "user-service 不可用，无法校验用户是否存在");
            }

            @Override
            public Result<Boolean> checkFamilyBind(Long communityId, Long familyUserId, Long elderUserId) {
                // 返回明确失败，不让亲情号越权校验静默放行。
                return Result.fail(ErrorCode.BIZ_ERROR, "user-service 不可用，无法校验亲情号绑定关系");
            }
        };
    }
}
