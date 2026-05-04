package com.eldercare.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 订单评价请求。
 *
 * @param score     评分，1 到 5 分。
 * @param tags      评价标签，第一版用逗号分隔的短文本保存。
 * @param content   评价正文。
 * @param anonymous 是否匿名展示。
 */
public record OrderEvaluateDTO(
        // 评分不能为空，业务上只允许 1 到 5 分。
        @NotNull(message = "评分不能为空") @Min(value = 1, message = "评分不能小于1") @Max(value = 5, message = "评分不能大于5") Integer score,
        // 标签是可选字段，例如“准时,耐心”。
        String tags,
        // 正文是可选字段，允许老人简单评分不写长文本。
        String content,
        // 是否匿名；为空时业务层按 false 处理。
        Boolean anonymous
) {
}
