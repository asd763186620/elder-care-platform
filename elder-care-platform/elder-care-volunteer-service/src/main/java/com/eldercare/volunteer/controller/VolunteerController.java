package com.eldercare.volunteer.controller;

import com.eldercare.api.dto.AvailableVolunteerQueryDTO;
import com.eldercare.api.dto.VolunteerAvailableTimeDTO;
import com.eldercare.api.dto.VolunteerProfileDTO;
import com.eldercare.api.vo.VolunteerBriefVO;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.response.Result;
import com.eldercare.volunteer.service.VolunteerAppService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 志愿者端业务接口。
 * 说明：这些接口由网关校验 JWT 后访问，当前用户信息从 common 的 UserContext 中读取。
 */
@RestController
@RequestMapping("/volunteers")
public class VolunteerController {
    /** 志愿者业务服务，封装资料、可用时间和可用志愿者查询逻辑。 */
    private final VolunteerAppService service;

    /**
     * 构造器注入业务服务，便于单元测试并避免字段注入。
     *
     * @param service 志愿者业务服务。
     */
    public VolunteerController(VolunteerAppService service) {
        this.service = service;
    }

    /**
     * 完善或更新当前登录志愿者资料。
     *
     * @param dto 前端提交的志愿者资料。
     * @return 空结果，成功时 code 为 0。
     */
    @PostMapping("/profile")
    @RequireRole(RoleConstants.VOLUNTEER)
    public Result<Void> profile(@Valid @RequestBody VolunteerProfileDTO dto) {
        service.saveProfile(dto);
        return Result.success();
    }

    /**
     * 设置当前志愿者的可服务时间。
     *
     * @param dto 可服务时间和服务项目。
     * @return 空结果，成功时 code 为 0。
     */
    @PostMapping("/available-times")
    @RequireRole(RoleConstants.VOLUNTEER)
    public Result<Void> availableTimes(@Valid @RequestBody VolunteerAvailableTimeDTO dto) {
        service.addAvailableTime(dto);
        return Result.success();
    }

    /**
     * 查询当前社区、指定服务项目和时间段下可接单的志愿者。
     *
     * @param serviceItemId 服务项目 ID。
     * @param startTime     预约开始时间，使用 ISO 日期时间格式。
     * @param endTime       预约结束时间，使用 ISO 日期时间格式。
     * @return 可用志愿者简要信息列表。
     */
    @GetMapping("/available")
    @RequireRole({RoleConstants.ELDER, RoleConstants.FAMILY})
    public Result<List<VolunteerBriefVO>> available(@RequestParam Long serviceItemId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(service.listAvailable(new AvailableVolunteerQueryDTO(serviceItemId, startTime, endTime)));
    }
}
