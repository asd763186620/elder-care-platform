package com.eldercare.api.client;

import com.eldercare.api.client.fallback.VolunteerFeignFallbackFactory;
import com.eldercare.api.dto.AvailableVolunteerQueryDTO;
import com.eldercare.api.dto.VolunteerCheckAvailableDTO;
import com.eldercare.api.dto.VolunteerLockTimeDTO;
import com.eldercare.api.vo.VolunteerBriefVO;
import com.eldercare.common.constant.ServiceNames;
import com.eldercare.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 志愿者服务 Feign 接口，供订单服务查询可用志愿者。
 */
@FeignClient(name = ServiceNames.VOLUNTEER_SERVICE, contextId = "volunteerFeignClient", fallbackFactory = VolunteerFeignFallbackFactory.class)
public interface VolunteerFeignClient {

    /**
     * 查询指定时间段可用的志愿者。
     *
     * @param query 查询条件。
     * @return 可用志愿者列表。
     */
    @PostMapping("/internal/volunteers/available")
    Result<List<VolunteerBriefVO>> listAvailable(@RequestBody AvailableVolunteerQueryDTO query);

    /**
     * 校验志愿者是否可用。
     */
    @PostMapping("/internal/volunteers/check-available")
    Result<Boolean> checkAvailable(@RequestBody VolunteerCheckAvailableDTO query);

    /**
     * 锁定志愿者时间。
     */
    @PostMapping("/internal/volunteers/lock-time")
    Result<Boolean> lockTime(@RequestBody VolunteerLockTimeDTO query);

    /**
     * 释放志愿者时间。
     */
    @PostMapping("/internal/volunteers/release-time")
    Result<Boolean> releaseTime(@RequestBody VolunteerLockTimeDTO query);
}
