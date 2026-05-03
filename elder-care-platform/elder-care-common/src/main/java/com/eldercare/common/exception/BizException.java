package com.eldercare.common.exception;

/**
 * 统一业务异常，后续业务服务主动抛出该异常表示可预期的业务失败。
 */
public class BizException extends RuntimeException {

    /**
     * 业务错误码。
     */
    private final ErrorCode errorCode;

    /**
     * 使用默认业务错误码创建异常。
     *
     * @param message 具体错误消息。
     */
    public BizException(String message) {
        // 默认使用通用业务错误码。
        this(ErrorCode.BIZ_ERROR, message);
    }

    /**
     * 使用指定错误码创建异常。
     *
     * @param errorCode 业务错误码。
     */
    public BizException(ErrorCode errorCode) {
        // 使用错误码默认消息。
        this(errorCode, errorCode.getMessage());
    }

    /**
     * 使用指定错误码和自定义消息创建异常。
     *
     * @param errorCode 业务错误码。
     * @param message   具体错误消息。
     */
    public BizException(ErrorCode errorCode, String message) {
        // 将具体错误消息交给 RuntimeException 保存。
        super(message);
        // 保存业务错误码，供全局异常处理器读取。
        this.errorCode = errorCode;
    }

    /**
     * 获取业务错误码。
     *
     * @return 业务错误码。
     */
    public ErrorCode getErrorCode() {
        // 返回构造异常时保存的错误码。
        return errorCode;
    }
}
