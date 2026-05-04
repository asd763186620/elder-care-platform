package com.eldercare.api.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 志愿者签到记录。
 */
public record VolunteerCheckinRecordVO(Long id,
                                       LocalDate checkinDate,
                                       LocalDateTime checkinTime,
                                       BigDecimal longitude,
                                       BigDecimal latitude,
                                       String address,
                                       Integer status) {
}
