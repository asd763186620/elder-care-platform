package com.eldercare.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.api.vo.CommunityVO;
import com.eldercare.api.vo.ServiceItemVO;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.community.entity.Community;
import com.eldercare.community.enums.CommunityStatusEnum;
import com.eldercare.community.entity.ServiceItem;
import com.eldercare.community.mapper.CommunityMapper;
import com.eldercare.community.mapper.ServiceItemMapper;
import com.eldercare.community.service.CommunityAppService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 社区服务业务实现。
 */
@Service
public class CommunityAppServiceImpl implements CommunityAppService {

    /**
     * 社区 Mapper。
     */
    private final CommunityMapper communityMapper;

    /**
     * 服务项目 Mapper。
     */
    private final ServiceItemMapper serviceItemMapper;

    /**
     * 构造业务服务。
     *
     * @param communityMapper   社区 Mapper。
     * @param serviceItemMapper 服务项目 Mapper。
     */
    public CommunityAppServiceImpl(CommunityMapper communityMapper, ServiceItemMapper serviceItemMapper) {
        // 保存社区 Mapper。
        this.communityMapper = communityMapper;
        // 保存服务项目 Mapper。
        this.serviceItemMapper = serviceItemMapper;
    }

    /**
     * 查询当前用户所属社区。
     */
    @Override
    public CommunityVO getCurrentCommunity() {
        // 从请求头加载并校验社区上下文。
        Long communityId = loadRequiredCommunityId();
        // 查询当前社区。
        Community community = selectCommunity(communityId);
        // 社区不存在时直接报错。
        if (community == null) {
            // 抛出资源不存在异常。
            throw new BizException(ErrorCode.NOT_FOUND, "当前社区不存在或已停用");
        }
        // 转换为 VO。
        return toCommunityVO(community);
    }

    /**
     * 查询当前社区下启用的服务项目。
     */
    @Override
    public List<ServiceItemVO> listServiceItems() {
        // 从请求头加载并校验社区上下文。
        Long communityId = loadRequiredCommunityId();
        // 查询当前社区下启用的服务项目。
        return serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getCommunityId, communityId)
                        .eq(ServiceItem::getItemStatus, CommunityStatusEnum.NORMAL.code())
                        .eq(ServiceItem::getDeleted, 0)
                        .orderByAsc(ServiceItem::getId))
                .stream()
                .map(this::toServiceItemVO)
                .toList();
    }

    /**
     * 查询当前社区下服务项目详情。
     */
    @Override
    public ServiceItemVO getServiceItem(Long id) {
        // 从请求头加载并校验社区上下文。
        Long communityId = loadRequiredCommunityId();
        // 按 community_id 和服务项目 ID 查询。
        ServiceItem serviceItem = selectServiceItem(communityId, id);
        // 不存在时说明项目不存在或不属于当前社区。
        if (serviceItem == null) {
            // 抛出资源不存在异常。
            throw new BizException(ErrorCode.NOT_FOUND, "服务项目不存在或不属于当前社区");
        }
        // 转换为 VO。
        return toServiceItemVO(serviceItem);
    }

    /**
     * 校验服务项目是否属于指定社区。
     */
    @Override
    public boolean checkServiceItem(Long communityId, Long serviceItemId) {
        // 参数缺失直接返回 false。
        if (communityId == null || serviceItemId == null) {
            // 校验失败。
            return false;
        }
        // 查询服务项目是否存在且启用。
        return selectServiceItem(communityId, serviceItemId) != null;
    }

    /**
     * 从请求头读取当前社区 ID。
     */
    private Long loadRequiredCommunityId() {
        // 从当前请求头加载用户上下文。
        UserContext.loadFromCurrentRequest();
        // 确保用户已登录。
        UserContext.requireLoginUser();
        // 返回社区 ID。
        return UserContext.requireCommunityId();
    }

    /**
     * 查询正常社区。
     */
    private Community selectCommunity(Long communityId) {
        // 使用 community_id 查询并限制正常状态。
        return communityMapper.selectOne(new LambdaQueryWrapper<Community>()
                .eq(Community::getCommunityId, communityId)
                .eq(Community::getCommunityStatus, CommunityStatusEnum.NORMAL.code())
                .eq(Community::getDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 查询正常服务项目。
     */
    private ServiceItem selectServiceItem(Long communityId, Long serviceItemId) {
        // 使用 community_id 和服务项目 ID 双条件，防止跨社区访问。
        return serviceItemMapper.selectOne(new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getCommunityId, communityId)
                .eq(ServiceItem::getId, serviceItemId)
                .eq(ServiceItem::getItemStatus, CommunityStatusEnum.NORMAL.code())
                .eq(ServiceItem::getDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 转换社区 VO。
     */
    private CommunityVO toCommunityVO(Community community) {
        // 组装接口返回对象。
        return new CommunityVO(
                community.getId(),
                community.getCommunityId(),
                community.getCommunityName(),
                community.getProvince(),
                community.getCity(),
                community.getDistrict(),
                community.getAddress(),
                community.getContactName(),
                community.getContactPhone()
        );
    }

    /**
     * 转换服务项目 VO。
     */
    private ServiceItemVO toServiceItemVO(ServiceItem serviceItem) {
        // 组装接口返回对象。
        return new ServiceItemVO(
                serviceItem.getId(),
                serviceItem.getCommunityId(),
                serviceItem.getItemName(),
                serviceItem.getItemCode(),
                serviceItem.getItemDesc(),
                serviceItem.getDurationMinutes(),
                serviceItem.getPriceCent(),
                serviceItem.getItemStatus()
        );
    }
}
