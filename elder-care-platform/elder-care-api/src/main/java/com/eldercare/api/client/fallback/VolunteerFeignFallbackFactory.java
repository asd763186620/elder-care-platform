package com.eldercare.api.client.fallback;

import com.eldercare.api.client.VolunteerFeignClient;
import com.eldercare.api.dto.AvailableVolunteerQueryDTO;
import com.eldercare.api.dto.VolunteerCheckAvailableDTO;
import com.eldercare.api.dto.VolunteerLockTimeDTO;
import com.eldercare.api.vo.VolunteerBriefVO;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.common.response.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * volunteer-service Feign 降级工厂。
 */
@Component
public class VolunteerFeignFallbackFactory implements FallbackFactory<VolunteerFeignClient> {
    /** 日志对象。 */
    private static final Logger log = LoggerFactory.getLogger(VolunteerFeignFallbackFactory.class);

    @Override
    public VolunteerFeignClient create(Throwable cause) {
        // 记录降级原因。
        log.error("volunteer-service feign fallback triggered", cause);
        // 返回降级实现。
        return new VolunteerFeignClient() {
            @Override
            public Result<List<VolunteerBriefVO>> listAvailable(AvailableVolunteerQueryDTO query) {
                return Result.fail(ErrorCode.BIZ_ERROR, "volunteer-service 不可用，无法查询可用志愿者");
            }

            @Override
            public Result<Boolean> checkAvailable(VolunteerCheckAvailableDTO query) {
                return Result.fail(ErrorCode.BIZ_ERROR, "volunteer-service 不可用，无法校验志愿者时间");
            }

            @Override
            public Result<Boolean> lockTime(VolunteerLockTimeDTO query) {
                return Result.fail(ErrorCode.BIZ_ERROR, "volunteer-service 不可用，无法锁定志愿者时间");
            }

            @Override
            public Result<Boolean> releaseTime(VolunteerLockTimeDTO query) {
                return Result.fail(ErrorCode.BIZ_ERROR, "volunteer-service 不可用，无法释放志愿者时间");
            }
        };
    }
}
