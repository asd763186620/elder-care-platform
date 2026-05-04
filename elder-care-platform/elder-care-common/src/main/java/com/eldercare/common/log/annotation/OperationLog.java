package com.eldercare.common.log.annotation;

import com.eldercare.common.log.enums.OperationModuleEnum;
import com.eldercare.common.log.enums.OperationTypeEnum;

import java.lang.annotation.*;

/**
 * 操作审计日志注解。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 业务模块。 */
    OperationModuleEnum module();

    /** 操作类型。 */
    OperationTypeEnum operationType();

    /** 操作描述。 */
    String description() default "";

    /** 业务 ID SpEL 表达式，例如 #orderId 或 #dto.id。 */
    String bizId() default "";

    /** 是否记录请求参数。 */
    boolean recordParams() default true;

    /** 是否记录响应结果。 */
    boolean recordResult() default false;
}
