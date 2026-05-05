package com.eldercare.user.controller;

import com.eldercare.api.dto.ElderCreateDTO;
import com.eldercare.api.vo.ElderProfileVO;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.enums.RoleEnum;
import com.eldercare.common.response.Result;
import com.eldercare.user.service.UserAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 老人资料控制器。
 */
@RestController
@RequestMapping("/elders")
@RequireRole(RoleEnum.ELDER)
@Validated
@Tag(name = "老人资料接口", description = "老人档案创建和维护")
public class ElderController {

    /**
     * 用户业务服务。
     */
    private final UserAppService userAppService;

    /**
     * 构造控制器。
     *
     * @param userAppService 用户业务服务。
     */
    public ElderController(UserAppService userAppService) {
        // 保存用户业务服务。
        this.userAppService = userAppService;
    }

    /**
     * 新增老人资料。
     *
     * @param createDTO 新增老人请求。
     * @return 老人资料。
     */
    @PostMapping
    @Operation(summary = "新增老人资料", description = "当前老人账号创建自己的老人档案")
    public Result<ElderProfileVO> create(@Valid @RequestBody ElderCreateDTO createDTO) {
        // 调用业务服务创建老人资料。
        return Result.success(userAppService.createElder(createDTO));
    }
}
