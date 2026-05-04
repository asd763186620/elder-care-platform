package com.eldercare.auth.wechat;

import com.eldercare.auth.config.WechatMiniAppProperties;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 微信 code2session 客户端。
 */
@Component
public class WechatCode2SessionClient {
    /** 微信 code2session 接口地址。 */
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
    /** 微信配置。 */
    private final WechatMiniAppProperties properties;
    /** HTTP 客户端。 */
    private final RestTemplate restTemplate = new RestTemplate();
    /** JSON 解析器。 */
    private final ObjectMapper objectMapper;

    public WechatCode2SessionClient(WechatMiniAppProperties properties, ObjectMapper objectMapper) {
        // 保存微信配置。
        this.properties = properties;
        // 保存 JSON 解析器。
        this.objectMapper = objectMapper;
    }

    /**
     * 使用小程序 code 换取微信会话。
     *
     * @param code 小程序 wx.login 返回的 code。
     * @return 微信会话。
     */
    public WechatSession code2Session(String code) {
        // 本地明确开启 mock 时才生成模拟 openId。
        if (properties.isMockEnabled()) {
            // 使用 code 派生稳定 openId，方便本地重复登录。
            return new WechatSession("mock-openid-" + code, null, "mock-session-key");
        }
        // 缺少 appId 或 secret 时不能调用真实微信。
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getSecret())) {
            // 抛出配置错误。
            throw new BizException(ErrorCode.SYSTEM_ERROR, "微信小程序 appId 或 secret 未配置");
        }
        try {
            // 微信接口有时返回 text/plain，这里先按字符串接收。
            String body = restTemplate.getForObject(CODE2SESSION_URL, String.class, properties.getAppId(), properties.getSecret(), code);
            // 解析 JSON 响应。
            Map<String, Object> response = objectMapper.readValue(body, new TypeReference<>() {});
            // 微信错误码存在时说明 code 无效或配置错误。
            if (response.get("errcode") != null) {
                // 抛出业务异常，保留错误码便于调试。
                throw new BizException(ErrorCode.UNAUTHORIZED, "微信登录失败：" + response.get("errcode") + " " + response.get("errmsg"));
            }
            // 读取 openid。
            String openId = String.valueOf(response.get("openid"));
            // openid 为空时响应不可信。
            if (!StringUtils.hasText(openId) || "null".equals(openId)) {
                // 抛出登录失败。
                throw new BizException(ErrorCode.UNAUTHORIZED, "微信登录未返回 openid");
            }
            // 返回微信会话。
            return new WechatSession(openId, response.get("unionid") == null ? null : String.valueOf(response.get("unionid")), response.get("session_key") == null ? null : String.valueOf(response.get("session_key")));
        } catch (BizException exception) {
            // 业务异常直接向上抛。
            throw exception;
        } catch (Exception exception) {
            // 网络或解析异常转换成统一业务异常。
            throw new BizException(ErrorCode.SYSTEM_ERROR, "调用微信登录接口失败");
        }
    }
}
