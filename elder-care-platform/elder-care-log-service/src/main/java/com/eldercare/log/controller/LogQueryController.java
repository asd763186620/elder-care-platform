package com.eldercare.log.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.enums.RoleEnum;
import com.eldercare.common.response.Result;
import com.eldercare.log.entity.ApiAccessLog;
import com.eldercare.log.entity.OperationLog;
import com.eldercare.log.service.LogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 日志查询接口。
 */
@RestController
@RequestMapping("/logs")
@RequireRole(RoleEnum.ADMIN)
@Tag(name = "日志查询接口", description = "接口访问日志、慢接口日志和操作审计日志查询")
public class LogQueryController {
    /** 日志查询服务。 */
    private final LogQueryService service;

    public LogQueryController(LogQueryService service) {
        // 保存日志查询服务。
        this.service = service;
    }

    /** 分页查询接口访问日志。 */
    @GetMapping("/api-access/page")
    @Operation(summary = "接口访问日志分页", description = "支持按 traceId、userId、requestUri 过滤")
    public Result<IPage<ApiAccessLog>> apiLogs(@RequestParam(required = false) Integer pageNo,
                                               @RequestParam(required = false) Integer pageSize,
                                               @RequestParam(required = false) String traceId,
                                               @RequestParam(required = false) Long userId,
                                               @RequestParam(required = false) String requestUri) {
        return Result.success(service.apiLogs(pageNo, pageSize, traceId, userId, requestUri));
    }

    /** 分页查询慢接口日志。 */
    @GetMapping("/api-access/slow/page")
    @Operation(summary = "慢接口日志分页", description = "查询 slowFlag=true 的接口日志")
    public Result<IPage<ApiAccessLog>> slowApiLogs(@RequestParam(required = false) Integer pageNo,
                                                   @RequestParam(required = false) Integer pageSize) {
        return Result.success(service.slowApiLogs(pageNo, pageSize));
    }

    /** 根据 userId 查询操作日志。 */
    @GetMapping("/operation/user/{userId}")
    @Operation(summary = "按用户查询操作日志", description = "分页查询指定用户的操作审计日志")
    public Result<IPage<OperationLog>> operationLogsByUser(@PathVariable Long userId,
                                                           @RequestParam(required = false) Integer pageNo,
                                                           @RequestParam(required = false) Integer pageSize) {
        return Result.success(service.operationLogsByUser(userId, pageNo, pageSize));
    }

    /** 根据 bizId 查询业务轨迹。 */
    @GetMapping("/operation/biz/{bizId}")
    @Operation(summary = "按业务ID查询操作轨迹", description = "查询某个订单或业务数据的操作轨迹")
    public Result<List<OperationLog>> operationLogsByBizId(@PathVariable String bizId) {
        return Result.success(service.operationLogsByBizId(bizId));
    }

    /** 根据 traceId 查询链路日志。 */
    @GetMapping("/trace/{traceId}")
    @Operation(summary = "按 traceId 查询链路日志", description = "同时返回接口访问日志和操作审计日志")
    public Result<Map<String, Object>> traceLogs(@PathVariable String traceId) {
        return Result.success(Map.of(
                "apiAccessLogs", service.apiLogsByTraceId(traceId),
                "operationLogs", service.operationLogsByTraceId(traceId)
        ));
    }
}
