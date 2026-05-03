package com.eldercare.common.exception;

/**
 * 全局错误码枚举，用于统一各服务的失败响应。
 */
public enum ErrorCode {

    /**
     * 参数校验失败，通常由前端传参缺失或格式错误触发。
     */
    PARAM_ERROR(400, "请求参数错误"),

    /**
     * 请求参数合法，但业务规则不允许继续执行。
     */
    BIZ_ERROR(4001, "业务处理失败"),

    /**
     * 当前请求没有登录态或 JWT 无效。
     */
    UNAUTHORIZED(401, "未登录或登录已过期"),

    /**
     * 当前登录用户没有访问该资源或执行该操作的权限。
     */
    FORBIDDEN(403, "无权限执行该操作"),

    /**
     * 请求缺少社区上下文。
     */
    COMMUNITY_REQUIRED(4031, "缺少社区上下文"),

    /**
     * 请求的业务资源不存在。
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 重复提交、重复抢单等幂等冲突场景。
     */
    REPEAT_SUBMIT(409, "请勿重复提交"),

    /**
     * 资源已经存在，例如重复绑定亲情号。
     */
    DATA_EXISTS(4090, "数据已存在"),

    /**
     * 业务状态冲突，例如志愿者时间冲突、订单状态不允许流转。
     */
    BUSINESS_CONFLICT(4091, "业务状态冲突"),

    /**
     * 志愿者服务时间冲突。
     */
    VOLUNTEER_TIME_CONFLICT(4092, "志愿者时间冲突"),

    /**
     * 订单状态不允许当前操作。
     */
    ORDER_STATUS_ERROR(4093, "订单状态不允许当前操作"),

    /**
     * 服务端未知异常，避免把底层异常细节直接暴露给前端。
     */
    SYSTEM_ERROR(500, "系统异常");

    /**
     * 业务状态码。
     */
    private final Integer code;

    /**
     * 默认错误消息。
     */
    private final String message;

    /**
     * 构造错误码枚举。
     *
     * @param code    业务状态码。
     * @param message 默认错误消息。
     */
    ErrorCode(Integer code, String message) {
        // 保存业务状态码。
        this.code = code;
        // 保存默认错误消息。
        this.message = message;
    }

    /**
     * 获取业务状态码。
     *
     * @return 业务状态码。
     */
    public Integer getCode() {
        // 返回枚举持有的状态码。
        return code;
    }

    /**
     * 获取默认错误消息。
     *
     * @return 默认错误消息。
     */
    public String getMessage() {
        // 返回枚举持有的错误消息。
        return message;
    }
}
