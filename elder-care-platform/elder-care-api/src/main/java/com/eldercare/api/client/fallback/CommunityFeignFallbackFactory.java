package com.eldercare.api.client.fallback;

import com.eldercare.api.client.CommunityFeignClient;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.common.response.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * community-service Feign 降级工厂。
 */
@Component
public class CommunityFeignFallbackFactory implements FallbackFactory<CommunityFeignClient> {
    /** 日志对象。 */
    private static final Logger log = LoggerFactory.getLogger(CommunityFeignFallbackFactory.class);

    @Override
    public CommunityFeignClient create(Throwable cause) {
        // 记录降级原因。
        log.error("community-service feign fallback triggered", cause);
        // 返回降级实现。
        return (communityId, serviceItemId) -> Result.fail(ErrorCode.BIZ_ERROR, "community-service 不可用，无法校验服务项目");
    }
}
