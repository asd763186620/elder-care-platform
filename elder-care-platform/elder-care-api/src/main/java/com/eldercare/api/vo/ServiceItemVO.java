package com.eldercare.api.vo;

/**
 * 服务项目 VO。
 *
 * @param id              服务项目 ID。
 * @param communityId     所属社区 ID。
 * @param itemName        服务项目名称。
 * @param itemCode        服务项目编码。
 * @param itemDesc        服务项目说明。
 * @param durationMinutes 默认服务时长，单位分钟。
 * @param priceCent       服务价格，单位分。
 * @param itemStatus      项目状态。
 */
public record ServiceItemVO(
        // 服务项目 ID。
        Long id,
        // 所属社区 ID。
        Long communityId,
        // 服务项目名称。
        String itemName,
        // 服务项目编码。
        String itemCode,
        // 服务项目说明。
        String itemDesc,
        // 默认服务时长。
        Integer durationMinutes,
        // 服务价格，单位分。
        Integer priceCent,
        // 项目状态：1启用，2停用。
        Integer itemStatus
) {
}
