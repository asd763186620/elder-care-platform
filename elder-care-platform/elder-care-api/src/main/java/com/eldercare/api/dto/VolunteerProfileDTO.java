package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 志愿者资料请求。
 *
 * @param volunteerName       志愿者姓名。
 * @param volunteerPhone      志愿者手机号。
 * @param skillTags           技能标签。
 * @param serviceRadiusMeter  服务半径，单位米。
 */
public record VolunteerProfileDTO(
        // 志愿者姓名；用于订单详情和可用志愿者列表展示。
        @NotBlank(message = "不能为空") String volunteerName,
        // 志愿者手机号；第一版仅保存，后续可用于短信通知。
        String volunteerPhone,
        // 技能标签；第一版使用逗号分隔文本，例如：陪诊,保洁。
        String skillTags,
        // 服务半径，单位米；后续可以结合老人地址做距离筛选。
        Integer serviceRadiusMeter) {
}
