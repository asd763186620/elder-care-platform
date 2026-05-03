package com.eldercare.user.wechat;

/**
 * 微信手机号解析结果。
 *
 * @param phoneNumber     用户绑定手机号，可能带区号。
 * @param purePhoneNumber 不带区号的手机号。
 * @param countryCode     国家区号。
 */
public record WechatPhoneNumber(String phoneNumber, String purePhoneNumber, String countryCode) {
}
