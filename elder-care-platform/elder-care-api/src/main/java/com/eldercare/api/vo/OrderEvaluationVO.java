package com.eldercare.api.vo;

import java.time.LocalDateTime;

/**
 * 订单评价展示对象。
 */
public record OrderEvaluationVO(Long id,
                                Long orderId,
                                Long volunteerUserId,
                                Integer score,
                                String tags,
                                String content,
                                Boolean anonymous,
                                LocalDateTime createTime) {
}
