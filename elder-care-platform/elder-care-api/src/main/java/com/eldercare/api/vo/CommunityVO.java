package com.eldercare.api.vo;

/**
 * 社区信息 VO。
 *
 * @param id            表主键 ID。
 * @param communityId   社区业务 ID。
 * @param communityName 社区名称。
 * @param province      省份。
 * @param city          城市。
 * @param district      区县。
 * @param address       详细地址。
 * @param contactName   联系人。
 * @param contactPhone  联系电话。
 */
public record CommunityVO(
        // 表主键 ID。
        Long id,
        // 社区业务 ID。
        Long communityId,
        // 社区名称。
        String communityName,
        // 省份。
        String province,
        // 城市。
        String city,
        // 区县。
        String district,
        // 详细地址。
        String address,
        // 联系人。
        String contactName,
        // 联系电话。
        String contactPhone
) {
}
