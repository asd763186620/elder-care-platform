package com.eldercare.api.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情响应。
 *
 * @param order             订单基础信息。
 * @param serviceItem       服务项目摘要，第一版跨服务查询失败时可以为空。
 * @param elder             老人摘要，第一版跨服务查询失败时可以为空。
 * @param familyUserId      代发亲情号用户 ID。
 * @param volunteer         志愿者摘要。
 * @param statusLogs        状态流转日志。
 */
public record OrderDetailVO(
        // 订单基础字段。
        OrderVO order,
        // 服务项目信息。
        ServiceItemVO serviceItem,
        // 老人档案信息。
        ElderProfileVO elder,
        // 代发亲情号用户 ID；老人本人下单时等于 creatorUserId 或为空。
        Long familyUserId,
        // 志愿者简要信息。
        VolunteerBriefVO volunteer,
        // 状态日志列表。
        List<StatusLogItem> statusLogs
) {
    /**
     * 状态日志条目。
     *
     * @param fromStatus     流转前状态。
     * @param toStatus       流转后状态。
     * @param operatorUserId 操作人 ID。
     * @param operatorRole   操作人角色。
     * @param operateType    操作类型。
     * @param operateRemark  操作备注。
     * @param createTime     创建时间。
     */
    public record StatusLogItem(String fromStatus,
                                String toStatus,
                                Long operatorUserId,
                                String operatorRole,
                                String operateType,
                                String operateRemark,
                                LocalDateTime createTime) {
    }
}
