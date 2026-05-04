package com.eldercare.auth.wechat;

/**
 * 微信 code2session 返回的登录会话。
 *
 * @param openId     小程序维度用户唯一标识。
 * @param unionId    开放平台唯一标识，未绑定开放平台时可能为空。
 * @param sessionKey 微信会话密钥，后端第一版不下发给前端。
 */
public record WechatSession(String openId, String unionId, String sessionKey) {
}
