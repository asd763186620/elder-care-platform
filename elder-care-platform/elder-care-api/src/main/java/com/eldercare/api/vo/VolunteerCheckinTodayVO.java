package com.eldercare.api.vo;

import java.time.LocalDateTime;

/**
 * 志愿者今日签到状态。
 *
 * @param checkedIn   是否已签到。
 * @param checkinTime 签到时间。
 * @param address     签到地址。
 */
public record VolunteerCheckinTodayVO(
        // true 表示今天已经签到。
        boolean checkedIn,
        // 签到时间；未签到时为空。
        LocalDateTime checkinTime,
        // 签到地址；未签到时为空。
        String address
) {
}
