package com.eldercare.user.wechat;

/**
 * 微信 code2session 返回的业务会话。
 *
 * @param openId     小程序 openId。
 * @param unionId    开放平台 unionId，可为空。
 * @param sessionKey 微信 session_key，第一版不落库。
 */
public record WechatSession(String openId, String unionId, String sessionKey) {
}
