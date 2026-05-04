package com.eldercare.api.vo;

/**
 * 志愿者评分信息。
 */
public record VolunteerScoreVO(Long volunteerUserId,
                               Integer score,
                               Long totalServiceCount,
                               Long evaluationCount) {
}
