package com.eldercare.user.controller;

import com.eldercare.api.dto.FamilyBindDTO;
import com.eldercare.api.vo.ElderProfileVO;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.response.Result;
import com.eldercare.user.service.UserAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 亲情号控制器。
 */
@RestController
@RequestMapping("/family")
@RequireRole(RoleConstants.FAMILY)
@Validated
@Tag(name = "亲情号接口", description = "亲情号绑定老人和查询绑定老人")
public class FamilyController {

    /**
     * 用户业务服务。
     */
    private final UserAppService userAppService;

    /**
     * 构造控制器。
     *
     * @param userAppService 用户业务服务。
     */
    public FamilyController(UserAppService userAppService) {
        // 保存用户业务服务。
        this.userAppService = userAppService;
    }

    /**
     * 亲情号绑定老人。
     *
     * @param bindDTO 绑定请求。
     * @return 成功响应。
     */
    @PostMapping("/bind")
    @Operation(summary = "绑定老人", description = "亲情号绑定同社区老人")
    public Result<Void> bind(@Valid @RequestBody FamilyBindDTO bindDTO) {
        // 调用业务服务绑定老人。
        userAppService.bindElder(bindDTO);
        // 返回成功响应。
        return Result.success();
    }

    /**
     * 查询当前亲情号绑定的老人列表。
     *
     * @return 老人列表。
     */
    @GetMapping("/elders")
    @Operation(summary = "查询绑定老人", description = "查询当前亲情号已经绑定的老人列表")
    public Result<List<ElderProfileVO>> elders() {
        // 调用业务服务查询绑定老人。
        return Result.success(userAppService.listBoundElders());
    }
}
