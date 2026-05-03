package com.eldercare.user.wechat;

import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.user.config.WechatMiniAppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * 微信小程序手机号解析客户端。
 */
@Component
public class WechatPhoneNumberClient {
    /** 微信 access_token 接口地址模板。 */
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";

    /** 微信手机号解析接口地址模板。 */
    private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={accessToken}";

    /** 微信 access_token Redis key。 */
    private static final String ACCESS_TOKEN_KEY = "wechat:miniapp:access_token";

    /** 微信小程序配置。 */
    private final WechatMiniAppProperties properties;

    /** Redis 客户端，用于缓存 access_token。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 简单 HTTP 客户端。 */
    private final RestTemplate restTemplate = new RestTemplate();

    /** JSON 解析器，用于解析微信 text/plain 响应体。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WechatPhoneNumberClient(WechatMiniAppProperties properties, StringRedisTemplate stringRedisTemplate) {
        // 保存微信配置。
        this.properties = properties;
        // 保存 Redis 客户端。
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 使用微信手机号 code 换取手机号。
     *
     * @param code 小程序 getPhoneNumber 返回的 code。
     * @return 微信手机号。
     */
    public WechatPhoneNumber getPhoneNumber(String code) {
        // 未配置真实微信参数时，默认直接失败；只有显式打开 mockEnabled 才允许本地 mock。
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getSecret())) {
            // mock 开关关闭时直接提示配置错误。
            if (!properties.isMockEnabled()) {
                // 抛出配置错误。
                throw new BizException(ErrorCode.BUSINESS_CONFLICT, "未配置微信小程序 appId/appSecret");
            }
            // mock code 如果本身像手机号，则直接当手机号返回。
            String mockPhone = code.matches("^1\\d{10}$") ? code : "18800000000";
            // 返回 mock 手机号。
            return new WechatPhoneNumber(mockPhone, mockPhone, "86");
        }
        // 获取小程序 access_token。
        String accessToken = accessToken();
        // 调用微信手机号解析接口；微信可能返回 text/plain，所以先按字符串接收。
        String responseBody = restTemplate.postForObject(PHONE_NUMBER_URL, Map.of("code", code), String.class, accessToken);
        // 将微信 JSON 字符串解析为 Map。
        Map<String, Object> response = parseJson(responseBody, "微信手机号响应解析失败");
        // 响应为空时认为微信接口异常。
        if (response == null) {
            // 抛出业务异常。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "微信手机号解析失败");
        }
        // 微信错误码非 0 时表示 code 无效或 access_token 无效。
        Object errCode = response.get("errcode");
        if (errCode != null && !"0".equals(String.valueOf(errCode))) {
            // 清理 access_token，下一次重新获取。
            stringRedisTemplate.delete(ACCESS_TOKEN_KEY);
            // 抛出微信错误。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "微信手机号解析失败：" + response.get("errmsg"));
        }
        // phone_info 是微信返回的手机号对象。
        Object phoneInfoObject = response.get("phone_info");
        // phone_info 类型不符合预期时直接报错。
        if (!(phoneInfoObject instanceof Map<?, ?> phoneInfo)) {
            // 抛出业务异常。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "微信手机号响应格式错误");
        }
        // 读取完整手机号。
        String phoneNumber = String.valueOf(phoneInfo.get("phoneNumber"));
        // 读取不带区号手机号。
        String purePhoneNumber = String.valueOf(phoneInfo.get("purePhoneNumber"));
        // 手机号为空时不能绑定。
        if (!StringUtils.hasText(purePhoneNumber) || "null".equals(purePhoneNumber)) {
            // 抛出业务异常。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "微信未返回手机号");
        }
        // 读取国家区号。
        String countryCode = String.valueOf(phoneInfo.get("countryCode"));
        // 返回手机号结果。
        return new WechatPhoneNumber(phoneNumber, purePhoneNumber, countryCode);
    }

    /**
     * 获取并缓存微信 access_token。
     *
     * @return access_token。
     */
    private String accessToken() {
        // 先从 Redis 读取缓存 token。
        String cached = stringRedisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        // 缓存存在时直接返回。
        if (StringUtils.hasText(cached)) {
            // 返回缓存 token。
            return cached;
        }
        // appId 和 secret 必须同时存在。
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getSecret())) {
            // 抛出配置错误。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "未配置微信小程序 appId/appSecret");
        }
        // 调用微信 access_token 接口。
        // 微信可能返回 content-type: text/plain，所以先按字符串接收。
        String responseBody = restTemplate.getForObject(ACCESS_TOKEN_URL, String.class, properties.getAppId(), properties.getSecret());
        // 将微信 JSON 字符串解析为 Map。
        Map<String, Object> response = parseJson(responseBody, "微信 access_token 响应解析失败");
        // 响应为空时认为微信接口异常。
        if (response == null) {
            // 抛出业务异常。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "微信 access_token 获取失败");
        }
        // 微信错误码非 0 时表示配置错误或接口异常。
        Object errCode = response.get("errcode");
        if (errCode != null && !"0".equals(String.valueOf(errCode))) {
            // 抛出微信错误。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "微信 access_token 获取失败：" + response.get("errmsg"));
        }
        // 读取 access_token。
        String accessToken = String.valueOf(response.get("access_token"));
        // token 为空时不能继续。
        if (!StringUtils.hasText(accessToken) || "null".equals(accessToken)) {
            // 抛出业务异常。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "微信未返回 access_token");
        }
        // 微信默认 7200 秒过期，这里缓存 7000 秒，避免临界过期。
        stringRedisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, accessToken, Duration.ofSeconds(7000));
        // 返回新 token。
        return accessToken;
    }

    /**
     * 解析微信接口 JSON 响应。
     *
     * @param body         微信响应体。
     * @param errorMessage 解析失败时的业务提示。
     * @return JSON Map。
     */
    private Map<String, Object> parseJson(String body, String errorMessage) {
        // 响应体为空时认为微信接口异常。
        if (!StringUtils.hasText(body)) {
            // 抛出业务异常。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, errorMessage);
        }
        try {
            // 微信响应是 JSON 文本，即使 content-type 是 text/plain 也可以正常解析。
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception exception) {
            // 解析失败时转换为业务异常，避免前端看到 500。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, errorMessage + "：" + body);
        }
    }
}
