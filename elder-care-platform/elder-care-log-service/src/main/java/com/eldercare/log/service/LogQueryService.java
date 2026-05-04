package com.eldercare.log.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.eldercare.log.entity.ApiAccessLog;
import com.eldercare.log.entity.OperationLog;

import java.util.List;

/**
 * 日志查询服务。
 */
public interface LogQueryService {
    /** 分页查询接口访问日志。 */
    IPage<ApiAccessLog> apiLogs(Integer pageNo, Integer pageSize, String traceId, Long userId, String requestUri);

    /** 分页查询慢接口日志。 */
    IPage<ApiAccessLog> slowApiLogs(Integer pageNo, Integer pageSize);

    /** 根据用户查询操作日志。 */
    IPage<OperationLog> operationLogsByUser(Long userId, Integer pageNo, Integer pageSize);

    /** 根据业务 ID 查询操作轨迹。 */
    List<OperationLog> operationLogsByBizId(String bizId);

    /** 根据 traceId 查询接口日志。 */
    List<ApiAccessLog> apiLogsByTraceId(String traceId);

    /** 根据 traceId 查询操作日志。 */
    List<OperationLog> operationLogsByTraceId(String traceId);
}
