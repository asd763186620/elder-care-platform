package com.eldercare.common.exception;

import com.eldercare.common.response.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，统一把异常转换成 Result 响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /** 日志对象，用于记录后端异常堆栈。 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理手动抛出的业务异常。
     *
     * @param exception 业务异常对象。
     * @return 统一失败响应。
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception) {
        // 业务异常通常是可预期错误，记录 warn 便于排查第三方接口返回。
        log.warn("业务异常：code={}, message={}", exception.getErrorCode().getCode(), exception.getMessage());
        // 使用异常里的错误码和具体消息返回给调用方。
        return Result.fail(exception.getErrorCode().getCode(), exception.getMessage());
    }

    /**
     * 处理 @Valid 请求体参数校验失败。
     *
     * @param exception 参数校验异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        // 取第一个字段错误，保持第一版响应简单明确。
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        // 返回参数错误状态码和具体字段提示。
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理表单对象绑定失败。
     *
     * @param exception 表单绑定异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        // 取第一个字段错误，便于前端直接展示。
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        // 返回参数错误状态码和具体字段提示。
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理单个请求参数校验失败。
     *
     * @param exception 参数约束异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        // 直接返回校验框架提供的错误说明。
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), exception.getMessage());
    }

    /**
     * 处理其他未预料异常。
     *
     * @param exception 原始异常对象。
     * @return 统一失败响应。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        // 未预期异常必须打印完整堆栈，方便本地和服务器排查 500。
        log.error("系统异常", exception);
        // 不把堆栈细节暴露给前端。
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
