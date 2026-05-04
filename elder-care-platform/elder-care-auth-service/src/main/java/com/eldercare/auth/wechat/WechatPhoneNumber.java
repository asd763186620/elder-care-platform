package com.eldercare.auth.wechat;

/**
 * 微信手机号解析结果。
 *
 * @param phoneNumber     带区号手机号。
 * @param purePhoneNumber 纯手机号。
 * @param countryCode     国家区号。
 */
public record WechatPhoneNumber(String phoneNumber, String purePhoneNumber, String countryCode) {
}
