package com.eldercare.volunteer.controller;

import com.eldercare.api.dto.AvailableVolunteerQueryDTO;
import com.eldercare.api.dto.VolunteerCheckAvailableDTO;
import com.eldercare.api.dto.VolunteerLockTimeDTO;
import com.eldercare.api.vo.VolunteerBriefVO;
import com.eldercare.common.response.Result;
import com.eldercare.volunteer.service.VolunteerAppService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 志愿者服务内部接口，供订单服务通过 Feign 查询可用志愿者。
 */
@RestController
@RequestMapping("/internal/volunteers")
public class InternalVolunteerController {
    /** 志愿者业务服务，内部接口仍复用同一套业务规则。 */
    private final VolunteerAppService service;

    /**
     * 构造器注入业务服务。
     *
     * @param service 志愿者业务服务。
     */
    public InternalVolunteerController(VolunteerAppService service) {
        this.service = service;
    }

    /**
     * 查询可用志愿者。
     *
     * @param query 查询条件。
     * @return 可用志愿者列表。
     */
    @PostMapping("/available")
    public Result<List<VolunteerBriefVO>> listAvailable(@RequestBody AvailableVolunteerQueryDTO query) {
        return Result.success(service.listAvailable(query));
    }

    /**
     * 校验指定志愿者在某个服务时间段是否可接单。
     * 说明：order-service 创建指定志愿者订单前会调用该接口。
     *
     * @param query 校验参数，包含社区、志愿者、服务项目和时间段。
     * @return true 表示可用，false 表示不可用。
     */
    @PostMapping("/check-available")
    public Result<Boolean> checkAvailable(@RequestBody VolunteerCheckAvailableDTO query) {
        return Result.success(service.checkAvailable(query));
    }

    /**
     * 锁定志愿者时间。
     * 说明：order-service 在指定派单或抢单成功后调用，内部使用 Redisson + MySQL 唯一索引双保险。
     *
     * @param query 锁定参数，包含订单、志愿者和时间段。
     * @return true 表示锁定成功，false 表示时间段已被占用。
     */
    @PostMapping("/lock-time")
    public Result<Boolean> lockTime(@RequestBody VolunteerLockTimeDTO query) {
        return Result.success(service.lockTime(query));
    }

    /**
     * 释放志愿者时间。
     * 说明：订单取消时调用，将有效时间锁置为释放状态。
     *
     * @param query 释放参数，至少包含订单、志愿者和时间段。
     * @return true 表示释放成功。
     */
    @PostMapping("/release-time")
    public Result<Boolean> releaseTime(@RequestBody VolunteerLockTimeDTO query) {
        return Result.success(service.releaseTime(query));
    }
}
