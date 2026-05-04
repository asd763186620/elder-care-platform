package com.eldercare.volunteer.controller;

import com.eldercare.api.dto.AvailableVolunteerQueryDTO;
import com.eldercare.api.dto.VolunteerAvailableTimeDTO;
import com.eldercare.api.dto.VolunteerCheckinDTO;
import com.eldercare.api.dto.VolunteerProfileDTO;
import com.eldercare.api.vo.CursorPageVO;
import com.eldercare.api.vo.VolunteerBriefVO;
import com.eldercare.api.vo.VolunteerCheckinRecordVO;
import com.eldercare.api.vo.VolunteerCheckinTodayVO;
import com.eldercare.api.vo.VolunteerWorkbenchVO;
import com.eldercare.common.annotation.RepeatSubmit;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.response.Result;
import com.eldercare.volunteer.service.VolunteerAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * 志愿者端业务接口。
 * 说明：这些接口由网关校验 JWT 后访问，当前用户信息从 common 的 UserContext 中读取。
 */
@RestController
@RequestMapping("/volunteers")
@Validated
@Tag(name = "志愿者接口", description = "志愿者资料、可服务时间和可用志愿者查询")
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
    @Operation(summary = "完善志愿者资料", description = "当前志愿者新增或更新自己的资料")
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
    @Operation(summary = "设置可服务时间", description = "志愿者设置某服务项目的可服务时间段")
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
    @Operation(summary = "查询可用志愿者", description = "按服务项目和时间段查询本社区可用志愿者")
    public Result<List<VolunteerBriefVO>> available(@RequestParam Long serviceItemId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(service.listAvailable(new AvailableVolunteerQueryDTO(serviceItemId, startTime, endTime)));
    }

    /**
     * 志愿者今日签到。
     */
    @PostMapping("/check-in")
    @RepeatSubmit(expireSeconds = 5)
    @RequireRole(RoleConstants.VOLUNTEER)
    @Operation(summary = "志愿者签到", description = "同一志愿者同一天只能签到一次，使用唯一索引防重")
    public Result<Void> checkIn(@Valid @RequestBody VolunteerCheckinDTO dto) {
        service.checkIn(dto);
        return Result.success();
    }

    /**
     * 今日签到状态。
     */
    @GetMapping("/check-in/today")
    @RequireRole(RoleConstants.VOLUNTEER)
    @Operation(summary = "今日签到状态", description = "查询当前志愿者今天是否已签到")
    public Result<VolunteerCheckinTodayVO> todayCheckin() {
        return Result.success(service.todayCheckin());
    }

    /**
     * 签到记录分页。
     */
    @GetMapping("/check-in/page")
    @RequireRole(RoleConstants.VOLUNTEER)
    @Operation(summary = "签到记录分页", description = "游标分页查询当前志愿者自己的签到记录")
    public Result<CursorPageVO<VolunteerCheckinRecordVO>> checkinPage(@RequestParam(required = false) Long lastId,
                                                                      @RequestParam(required = false) Integer size,
                                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(service.checkinPage(lastId, size, startDate, endDate));
    }

    /**
     * 志愿者工作台。
     */
    @GetMapping("/workbench")
    @RequireRole(RoleConstants.VOLUNTEER)
    @Operation(summary = "志愿者工作台", description = "返回签到状态、接单状态和基础工作台统计")
    public Result<VolunteerWorkbenchVO> workbench() {
        return Result.success(service.workbench());
    }
}
