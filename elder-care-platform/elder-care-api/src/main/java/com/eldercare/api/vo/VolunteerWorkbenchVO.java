package com.eldercare.api.vo;

/**
 * 志愿者小程序工作台。
 */
public record VolunteerWorkbenchVO(boolean todayCheckedIn,
                                   long todayWaitServiceCount,
                                   long inServiceCount,
                                   long waitConfirmCount,
                                   long totalServiceCount,
                                   Integer score,
                                   String acceptOrderStatus) {
}
