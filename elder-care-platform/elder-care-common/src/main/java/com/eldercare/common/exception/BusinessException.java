package com.eldercare.common.exception;

/**
 * 业务异常，主动抛出后会被全局异常处理器转换为统一返回对象。
 */
public class BusinessException extends BizException {

    /**
     * 使用错误码的默认消息创建业务异常。
     *
     * @param errorCode 错误码枚举。
     */
    public BusinessException(ErrorCode errorCode) {
        // 复用新的 BizException 实现，保留旧类名兼容历史代码。
        super(errorCode);
    }

    /**
     * 使用错误码和自定义消息创建业务异常。
     *
     * @param errorCode 错误码枚举。
     * @param message   当前场景下更具体的错误消息。
     */
    public BusinessException(ErrorCode errorCode, String message) {
        // 复用新的 BizException 实现，保留旧类名兼容历史代码。
        super(errorCode, message);
    }
}
