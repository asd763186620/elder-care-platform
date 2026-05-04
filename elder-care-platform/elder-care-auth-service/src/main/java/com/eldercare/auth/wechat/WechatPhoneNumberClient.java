package com.eldercare.auth.wechat;

import com.eldercare.auth.config.WechatMiniAppProperties;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * 微信手机号解析客户端。
 */
@Component
public class WechatPhoneNumberClient {
    /** 获取微信 access_token 接口。 */
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    /** 获取手机号接口。 */
    private static final String PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={accessToken}";
    /** 微信配置。 */
    private final WechatMiniAppProperties properties;
    /** Redis 客户端。 */
    private final StringRedisTemplate redisTemplate;
    /** HTTP 客户端。 */
    private final RestTemplate restTemplate = new RestTemplate();
    /** JSON 解析器。 */
    private final ObjectMapper objectMapper;

    public WechatPhoneNumberClient(WechatMiniAppProperties properties, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        // 保存微信配置。
        this.properties = properties;
        // 保存 Redis 客户端。
        this.redisTemplate = redisTemplate;
        // 保存 JSON 解析器。
        this.objectMapper = objectMapper;
    }

    /**
     * 使用微信手机号 code 获取可信手机号。
     */
    public WechatPhoneNumber getPhoneNumber(String code) {
        // 本地 mock 时直接把 code 当手机号后缀使用。
        if (properties.isMockEnabled()) {
            // 返回模拟手机号。
            return new WechatPhoneNumber("1380000" + code.substring(Math.max(0, code.length() - 4)), "1380000" + code.substring(Math.max(0, code.length() - 4)), "86");
        }
        // 获取 accessToken。
        String accessToken = accessToken();
        try {
            // 组装请求体。
            Map<String, String> request = Map.of("code", code);
            // 微信接口有时返回 text/plain，这里先按字符串接收。
            String body = restTemplate.postForObject(PHONE_URL, request, String.class, accessToken);
            // 解析响应。
            Map<String, Object> response = objectMapper.readValue(body, new TypeReference<>() {});
            // errcode 非 0 表示失败。
            if (!"0".equals(String.valueOf(response.get("errcode")))) {
                // 抛出业务异常。
                throw new BizException(ErrorCode.UNAUTHORIZED, "微信手机号解析失败：" + response.get("errcode") + " " + response.get("errmsg"));
            }
            // 读取 phone_info。
            Map<?, ?> phoneInfo = (Map<?, ?>) response.get("phone_info");
            // phone_info 为空时响应无效。
            if (phoneInfo == null) {
                // 抛出解析异常。
                throw new BizException(ErrorCode.UNAUTHORIZED, "微信手机号响应为空");
            }
            // 返回手机号。
            return new WechatPhoneNumber(String.valueOf(phoneInfo.get("phoneNumber")), String.valueOf(phoneInfo.get("purePhoneNumber")), String.valueOf(phoneInfo.get("countryCode")));
        } catch (BizException exception) {
            // 业务异常直接抛出。
            throw exception;
        } catch (Exception exception) {
            // 其他异常转换成统一系统异常。
            throw new BizException(ErrorCode.SYSTEM_ERROR, "调用微信手机号接口失败");
        }
    }

    /**
     * 获取并缓存微信 access_token。
     */
    private String accessToken() {
        // Redis key 按 appId 隔离。
        String key = "wechat:access-token:" + properties.getAppId();
        // 优先读取缓存。
        String cached = redisTemplate.opsForValue().get(key);
        // 缓存存在时直接返回。
        if (StringUtils.hasText(cached)) {
            // 返回缓存 token。
            return cached;
        }
        // 缺少配置时不能请求微信。
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getSecret())) {
            // 抛出配置错误。
            throw new BizException(ErrorCode.SYSTEM_ERROR, "微信小程序 appId 或 secret 未配置");
        }
        try {
            // 调用微信 token 接口。
            String body = restTemplate.getForObject(ACCESS_TOKEN_URL, String.class, properties.getAppId(), properties.getSecret());
            // 解析 JSON。
            Map<String, Object> response = objectMapper.readValue(body, new TypeReference<>() {});
            // 错误码存在时说明请求失败。
            if (response.get("errcode") != null) {
                // 抛出业务异常。
                throw new BizException(ErrorCode.SYSTEM_ERROR, "微信 accessToken 获取失败：" + response.get("errcode") + " " + response.get("errmsg"));
            }
            // 读取 access_token。
            String accessToken = String.valueOf(response.get("access_token"));
            // access_token 为空时响应异常。
            if (!StringUtils.hasText(accessToken) || "null".equals(accessToken)) {
                // 抛出异常。
                throw new BizException(ErrorCode.SYSTEM_ERROR, "微信 accessToken 为空");
            }
            // 缓存 7000 秒，略小于微信默认 7200 秒有效期。
            redisTemplate.opsForValue().set(key, accessToken, Duration.ofSeconds(7000));
            // 返回 token。
            return accessToken;
        } catch (BizException exception) {
            // 业务异常直接抛出。
            throw exception;
        } catch (Exception exception) {
            // 网络或解析异常转换成系统异常。
            throw new BizException(ErrorCode.SYSTEM_ERROR, "调用微信 accessToken 接口失败");
        }
    }
}
