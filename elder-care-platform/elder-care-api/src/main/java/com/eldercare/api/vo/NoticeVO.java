package com.eldercare.api.vo;

import java.time.LocalDateTime;

/**
 * 小程序消息中心记录。
 */
public record NoticeVO(Long id,
                       String title,
                       String content,
                       String noticeType,
                       Integer readStatus,
                       String bizType,
                       Long bizId,
                       LocalDateTime createTime) {
}
