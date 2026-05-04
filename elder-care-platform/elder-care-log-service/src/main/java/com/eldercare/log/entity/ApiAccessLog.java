package com.eldercare.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eldercare.common.log.dto.ApiAccessLogMessage;

import java.time.LocalDateTime;

/**
 * 接口访问日志实体。
 */
@TableName("api_access_log")
public class ApiAccessLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private Long userId;
    private String roleType;
    private Long communityId;
    private String requestMethod;
    private String requestUri;
    private String queryParams;
    private String clientIp;
    private String userAgent;
    private Integer statusCode;
    private Long costTime;
    private Boolean slowFlag;
    private String errorMessage;
    private LocalDateTime requestTime;

    /** 从 MQ 消息创建实体。 */
    public static ApiAccessLog from(ApiAccessLogMessage message) {
        // 创建实体。
        ApiAccessLog log = new ApiAccessLog();
        // 映射字段。
        log.traceId = message.traceId();
        log.userId = message.userId();
        log.roleType = message.roleType();
        log.communityId = message.communityId();
        log.requestMethod = message.requestMethod();
        log.requestUri = message.requestUri();
        log.queryParams = message.queryParams();
        log.clientIp = message.clientIp();
        log.userAgent = message.userAgent();
        log.statusCode = message.statusCode();
        log.costTime = message.costTime();
        log.slowFlag = message.slowFlag();
        log.errorMessage = message.errorMessage();
        log.requestTime = message.requestTime();
        // 返回实体。
        return log;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String requestUri) { this.requestUri = requestUri; }
    public String getQueryParams() { return queryParams; }
    public void setQueryParams(String queryParams) { this.queryParams = queryParams; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public Long getCostTime() { return costTime; }
    public void setCostTime(Long costTime) { this.costTime = costTime; }
    public Boolean getSlowFlag() { return slowFlag; }
    public void setSlowFlag(Boolean slowFlag) { this.slowFlag = slowFlag; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public void setRequestTime(LocalDateTime requestTime) { this.requestTime = requestTime; }
}
