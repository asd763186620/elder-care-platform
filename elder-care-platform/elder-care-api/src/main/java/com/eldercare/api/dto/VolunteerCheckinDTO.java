package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 志愿者签到请求。
 *
 * @param longitude 签到经度。
 * @param latitude  签到纬度。
 * @param address   签到地址文本。
 */
public record VolunteerCheckinDTO(
        // 经度不能为空，用 BigDecimal 避免浮点精度误差。
        @NotNull(message = "经度不能为空") BigDecimal longitude,
        // 纬度不能为空，用 BigDecimal 避免浮点精度误差。
        @NotNull(message = "纬度不能为空") BigDecimal latitude,
        // 地址不能为空，方便小程序工作台直接展示。
        @NotBlank(message = "签到地址不能为空") @Size(max = 255, message = "不能超过255个字符") String address
) {
}
