package com.eldercare.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.log.entity.ApiAccessLog;
import com.eldercare.log.entity.OperationLog;
import com.eldercare.log.mapper.ApiAccessLogMapper;
import com.eldercare.log.mapper.OperationLogMapper;
import com.eldercare.log.service.LogQueryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 日志查询服务实现。
 */
@Service
public class LogQueryServiceImpl implements LogQueryService {
    /** 接口日志 Mapper。 */
    private final ApiAccessLogMapper apiMapper;
    /** 操作日志 Mapper。 */
    private final OperationLogMapper operationMapper;

    public LogQueryServiceImpl(ApiAccessLogMapper apiMapper, OperationLogMapper operationMapper) {
        // 保存接口日志 Mapper。
        this.apiMapper = apiMapper;
        // 保存操作日志 Mapper。
        this.operationMapper = operationMapper;
    }

    @Override
    public IPage<ApiAccessLog> apiLogs(Integer pageNo, Integer pageSize, String traceId, Long userId, String requestUri) {
        // 构造查询条件。
        QueryWrapper<ApiAccessLog> qw = new QueryWrapper<ApiAccessLog>().orderByDesc("id");
        // traceId 可选。
        if (StringUtils.hasText(traceId)) qw.eq("trace_id", traceId);
        // userId 可选。
        if (userId != null) qw.eq("user_id", userId);
        // requestUri 可选，使用 like 方便排查。
        if (StringUtils.hasText(requestUri)) qw.like("request_uri", requestUri);
        // 分页查询。
        return apiMapper.selectPage(page(pageNo, pageSize), qw);
    }

    @Override
    public IPage<ApiAccessLog> slowApiLogs(Integer pageNo, Integer pageSize) {
        // 查询 slow_flag=1 的接口日志。
        return apiMapper.selectPage(page(pageNo, pageSize), new QueryWrapper<ApiAccessLog>().eq("slow_flag", 1).orderByDesc("id"));
    }

    @Override
    public IPage<OperationLog> operationLogsByUser(Long userId, Integer pageNo, Integer pageSize) {
        // 查询指定用户操作日志。
        return operationMapper.selectPage(page(pageNo, pageSize), new QueryWrapper<OperationLog>().eq("user_id", userId).orderByDesc("id"));
    }

    @Override
    public List<OperationLog> operationLogsByBizId(String bizId) {
        // 查询业务轨迹。
        return operationMapper.selectList(new QueryWrapper<OperationLog>().eq("biz_id", bizId).orderByAsc("id"));
    }

    @Override
    public List<ApiAccessLog> apiLogsByTraceId(String traceId) {
        // 查询一次链路的接口日志。
        return apiMapper.selectList(new QueryWrapper<ApiAccessLog>().eq("trace_id", traceId).orderByAsc("id"));
    }

    @Override
    public List<OperationLog> operationLogsByTraceId(String traceId) {
        // 查询一次链路的操作日志。
        return operationMapper.selectList(new QueryWrapper<OperationLog>().eq("trace_id", traceId).orderByAsc("id"));
    }

    /** 创建分页对象。 */
    private <T> Page<T> page(Integer pageNo, Integer pageSize) {
        // 页码默认 1。
        long current = pageNo == null || pageNo < 1 ? 1 : pageNo;
        // 每页默认 10，最大 100。
        long size = Math.min(pageSize == null || pageSize < 1 ? 10 : pageSize, 100);
        // 返回 MyBatis-Plus 分页对象。
        return new Page<>(current, size);
    }
}
