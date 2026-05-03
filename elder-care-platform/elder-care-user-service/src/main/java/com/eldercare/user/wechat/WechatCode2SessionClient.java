package com.eldercare.user.wechat;

import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.user.config.WechatMiniAppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 微信小程序 code2session 客户端。
 */
@Component
@EnableConfigurationProperties(WechatMiniAppProperties.class)
public class WechatCode2SessionClient {
    /** 微信 code2session 接口地址模板。 */
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    /** 微信小程序配置。 */
    private final WechatMiniAppProperties properties;

    /** 简单 HTTP 客户端。 */
    private final RestTemplate restTemplate = new RestTemplate();

    /** JSON 解析器，用于解析微信 text/plain 响应体。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WechatCode2SessionClient(WechatMiniAppProperties properties) {
        // 保存微信配置。
        this.properties = properties;
    }

    /**
     * 使用小程序 code 换取微信会话。
     *
     * @param code 小程序登录 code。
     * @return 微信会话。
     */
    public WechatSession code2Session(String code) {
        // 未配置真实微信参数时，默认直接失败；只有显式打开 mockEnabled 才允许本地 mock。
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getSecret())) {
            // mock 开关关闭时直接提示配置错误。
            if (!properties.isMockEnabled()) {
                // 抛出配置错误。
                throw new BizException(ErrorCode.BUSINESS_CONFLICT, "未配置微信小程序 appId/appSecret");
            }
            // 使用 code 构造稳定 openId，仅用于显式打开 mockEnabled 的开发环境。
            return new WechatSession("mock_openid_" + code, "mock_unionid_" + code, "mock_session_key");
        }
        // 调用微信官方 code2session 接口；微信可能返回 content-type: text/plain，所以先按字符串接收。
        String responseBody = restTemplate.getForObject(CODE2SESSION_URL, String.class, properties.getAppId(), properties.getSecret(), code);
        // 将微信 JSON 字符串解析为 Map。
        Map<String, Object> response = parseJson(responseBody, "微信登录响应解析失败");
        // 响应为空时认为微信接口异常。
        if (response == null) {
            // 抛出业务异常。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "微信登录失败");
        }
        // 微信错误码非 0 时表示 code 无效或配置错误。
        Object errCode = response.get("errcode");
        if (errCode != null && !"0".equals(String.valueOf(errCode))) {
            // 将微信错误信息透出给调用方。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "微信登录失败：" + response.get("errmsg"));
        }
        // 读取 openId。
        String openId = String.valueOf(response.get("openid"));
        // openId 缺失时不能建立账号。
        if (!StringUtils.hasText(openId) || "null".equals(openId)) {
            // 抛出业务异常。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "微信登录未返回 openId");
        }
        // 读取 unionId，未绑定开放平台时可能为空。
        String unionId = response.get("unionid") == null ? null : String.valueOf(response.get("unionid"));
        // 读取 session_key，第一版只返回不落库。
        String sessionKey = response.get("session_key") == null ? null : String.valueOf(response.get("session_key"));
        // 返回标准业务会话。
        return new WechatSession(openId, unionId, sessionKey);
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
