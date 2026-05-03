package com.eldercare.common.response;

import com.eldercare.common.exception.ErrorCode;

/**
 * 统一接口返回对象，所有服务对外返回都建议使用该结构。
 *
 * @param code    业务状态码，0 表示成功，非 0 表示失败。
 * @param message 响应说明，成功时通常为 success，失败时为错误原因。
 * @param data    真实业务数据，列表、详情、分页对象都放在这里。
 * @param <T>     data 的泛型类型，让调用方能获得明确的数据结构。
 */
public record Result<T>(Integer code, String message, T data) {

    /**
     * 成功业务码。
     */
    private static final Integer SUCCESS_CODE = 200;

    /**
     * 成功默认消息。
     */
    private static final String SUCCESS_MESSAGE = "success";

    /**
     * 创建无数据的成功响应。
     *
     * @param <T> 返回数据泛型，占位使用。
     * @return 标准成功结果。
     */
    public static <T> Result<T> success() {
        // 成功响应的业务状态码统一为 0。
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 创建带数据的成功响应。
     *
     * @param data 需要返回给前端或调用方的业务数据。
     * @param <T>  数据类型。
     * @return 标准成功结果。
     */
    public static <T> Result<T> success(T data) {
        // 将调用方传入的数据放入 data 字段。
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 创建带自定义消息和数据的成功响应。
     *
     * @param message 成功提示。
     * @param data    业务数据。
     * @param <T>     数据类型。
     * @return 标准成功结果。
     */
    public static <T> Result<T> success(String message, T data) {
        // 自定义成功消息，适合创建成功、操作成功等场景。
        return new Result<>(SUCCESS_CODE, message, data);
    }

    /**
     * 根据错误码枚举创建失败响应。
     *
     * @param errorCode 统一维护的错误码枚举。
     * @param <T>       返回数据泛型，占位使用。
     * @return 标准失败结果。
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        // 失败响应不携带业务数据。
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 根据错误码和自定义消息创建失败响应。
     *
     * @param errorCode 错误码枚举。
     * @param message   当前场景下的具体错误消息。
     * @param <T>       返回数据泛型，占位使用。
     * @return 标准失败结果。
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        // 使用错误码的 code 和调用方提供的 message。
        return new Result<>(errorCode.getCode(), message, null);
    }

    /**
     * 根据自定义状态码和消息创建失败响应。
     *
     * @param code    业务状态码。
     * @param message 错误消息。
     * @param <T>     返回数据泛型，占位使用。
     * @return 标准失败结果。
     */
    public static <T> Result<T> fail(Integer code, String message) {
        // 失败响应不携带业务数据。
        return new Result<>(code, message, null);
    }
}
