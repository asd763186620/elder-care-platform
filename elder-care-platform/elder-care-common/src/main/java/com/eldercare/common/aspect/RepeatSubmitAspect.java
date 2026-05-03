package com.eldercare.common.aspect;

import com.eldercare.common.annotation.RepeatSubmit;
import com.eldercare.common.constant.RedisKeyConstants;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.context.UserInfoDTO;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

/**
 * 防重复提交切面。
 * 说明：该切面只拦截标记了 @RepeatSubmit 的接口方法，并使用 Redis setIfAbsent 实现短时间幂等。
 */
@Aspect
public class RepeatSubmitAspect {

    /** Redis 字符串客户端，用于写入防重复提交 Key。 */
    private final StringRedisTemplate redisTemplate;

    /** Jackson 序列化工具，用于把请求参数稳定转换成字符串后计算摘要。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造防重复提交切面。
     *
     * @param redisTemplate Redis 字符串客户端。
     * @param objectMapper  Jackson 序列化工具。
     */
    public RepeatSubmitAspect(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        // 保存 Redis 客户端。
        this.redisTemplate = redisTemplate;
        // 保存 JSON 序列化工具。
        this.objectMapper = objectMapper;
    }

    /**
     * 环绕拦截带 @RepeatSubmit 的方法。
     *
     * @param joinPoint    当前被调用的方法。
     * @param repeatSubmit 方法上的注解配置。
     * @return 原方法返回值。
     * @throws Throwable 原方法异常。
     */
    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        // 从请求头读取并写入当前线程用户上下文，保证切面早于业务方法也能拿到 userId。
        UserInfoDTO userInfo = UserContext.loadFromCurrentRequest();
        // 未登录时直接抛出 401，避免匿名用户共用重复提交 Key。
        if (userInfo == null || userInfo.userId() == null) {
            // 返回统一未登录错误。
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        // 获取当前 Servlet 请求，用于读取 URI。
        HttpServletRequest request = currentRequest();
        // 计算请求体和参数摘要。
        String requestHash = hash(joinPoint.getArgs());
        // 按需求生成 Redis Key：repeat:{userId}:{uri}:{requestHash}。
        String key = RedisKeyConstants.REPEAT_SUBMIT + userInfo.userId() + ":" + request.getRequestURI() + ":" + requestHash;
        // 通过 setIfAbsent 实现“第一次成功写入，重复提交写入失败”。
        Boolean stored = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(repeatSubmit.expireSeconds()));
        // stored 为 false 表示同一个幂等窗口内已经提交过相同请求。
        if (!Boolean.TRUE.equals(stored)) {
            // 抛出重复提交异常。
            throw new BizException(ErrorCode.REPEAT_SUBMIT, repeatSubmit.message());
        }
        // 放行原方法。
        return joinPoint.proceed();
    }

    /**
     * 获取当前 Servlet 请求。
     *
     * @return 当前 HTTP 请求。
     */
    private HttpServletRequest currentRequest() {
        // 从 Spring 请求上下文中取 Servlet 请求属性。
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 理论上切面只在 Servlet 请求中生效，这里做兜底校验。
        if (attributes == null) {
            // 非 HTTP 请求不能执行防重复提交。
            throw new BizException(ErrorCode.SYSTEM_ERROR, "无法获取当前请求");
        }
        // 返回当前请求对象。
        return attributes.getRequest();
    }

    /**
     * 计算请求参数摘要。
     *
     * @param args Controller 方法参数。
     * @return MD5 摘要字符串。
     */
    private String hash(Object[] args) {
        // 过滤掉 request、response、文件等不适合直接序列化的对象。
        Object[] safeArgs = Arrays.stream(args)
                .filter(arg -> !(arg instanceof ServletRequest))
                .filter(arg -> !(arg instanceof ServletResponse))
                .filter(arg -> !(arg instanceof MultipartFile))
                .toArray();
        // 将安全参数转换成字符串。
        String text = toJson(safeArgs);
        // 对参数字符串计算 MD5，避免 Redis Key 过长。
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将参数序列化为 JSON。
     *
     * @param value 请求参数对象。
     * @return JSON 字符串；序列化失败时使用 toString 兜底。
     */
    private String toJson(Object value) {
        try {
            // 使用 Jackson 保证对象字段序列化结果稳定。
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            // 序列化失败时兜底使用 String.valueOf，避免防重切面阻断业务。
            return String.valueOf(value);
        }
    }
}
