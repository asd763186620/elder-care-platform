package com.eldercare.community.service;

import com.eldercare.api.vo.CommunityVO;
import com.eldercare.api.vo.ServiceItemVO;

import java.util.List;

/**
 * 社区服务业务接口。
 */
public interface CommunityAppService {

    /**
     * 查询当前用户所属社区。
     *
     * @return 当前社区信息。
     */
    CommunityVO getCurrentCommunity();

    /**
     * 查询当前社区下启用的服务项目。
     *
     * @return 服务项目列表。
     */
    List<ServiceItemVO> listServiceItems();

    /**
     * 查询当前社区下服务项目详情。
     *
     * @param id 服务项目 ID。
     * @return 服务项目详情。
     */
    ServiceItemVO getServiceItem(Long id);

    /**
     * 校验服务项目是否属于指定社区。
     *
     * @param communityId   社区 ID。
     * @param serviceItemId 服务项目 ID。
     * @return true 表示存在且属于该社区。
     */
    boolean checkServiceItem(Long communityId, Long serviceItemId);
}
