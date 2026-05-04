package com.eldercare.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eldercare.common.log.dto.OperationLogMessage;

import java.time.LocalDateTime;

/**
 * 操作审计日志实体。
 */
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private Long userId;
    private String roleType;
    private Long communityId;
    private String module;
    private String operationType;
    private String description;
    private String bizId;
    private String className;
    private String methodName;
    private String requestParams;
    private String responseResult;
    private Boolean successFlag;
    private String errorMessage;
    private Long costTime;
    private LocalDateTime operationTime;

    /** 从 MQ 消息创建实体。 */
    public static OperationLog from(OperationLogMessage message) {
        // 创建实体。
        OperationLog log = new OperationLog();
        // 映射字段。
        log.traceId = message.traceId();
        log.userId = message.userId();
        log.roleType = message.roleType();
        log.communityId = message.communityId();
        log.module = message.module();
        log.operationType = message.operationType();
        log.description = message.description();
        log.bizId = message.bizId();
        log.className = message.className();
        log.methodName = message.methodName();
        log.requestParams = message.requestParams();
        log.responseResult = message.responseResult();
        log.successFlag = message.successFlag();
        log.errorMessage = message.errorMessage();
        log.costTime = message.costTime();
        log.operationTime = message.operationTime();
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
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBizId() { return bizId; }
    public void setBizId(String bizId) { this.bizId = bizId; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
    public String getResponseResult() { return responseResult; }
    public void setResponseResult(String responseResult) { this.responseResult = responseResult; }
    public Boolean getSuccessFlag() { return successFlag; }
    public void setSuccessFlag(Boolean successFlag) { this.successFlag = successFlag; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getCostTime() { return costTime; }
    public void setCostTime(Long costTime) { this.costTime = costTime; }
    public LocalDateTime getOperationTime() { return operationTime; }
    public void setOperationTime(LocalDateTime operationTime) { this.operationTime = operationTime; }
}
