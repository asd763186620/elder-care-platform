package com.eldercare.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.eldercare.api.vo.OrderVO;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.response.Result;
import com.eldercare.order.entity.ServiceOrder;
import com.eldercare.order.mapper.ServiceOrderMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台订单管理接口。
 */
@RestController
@RequestMapping("/admin/orders")
@RequireRole(RoleConstants.ADMIN)
public class OrderAdminController {
    /** 订单 Mapper。 */
    private final ServiceOrderMapper orderMapper;

    public OrderAdminController(ServiceOrderMapper orderMapper) {
        // 保存订单 Mapper。
        this.orderMapper = orderMapper;
    }

    /**
     * 查询当前社区订单列表，供后台人工检索和干预前确认。
     *
     * @return 订单列表。
     */
    @GetMapping
    public Result<List<OrderVO>> list() {
        // 从请求头加载管理员上下文。
        UserContext.loadFromCurrentRequest();
        // 读取当前社区 ID。
        Long communityId = UserContext.requireCommunityId();
        // 查询当前社区最近 100 条订单。
        List<OrderVO> orders = orderMapper.selectList(new QueryWrapper<ServiceOrder>()
                        .eq("community_id", communityId)
                        .eq("deleted", 0)
                        .orderByDesc("id")
                        .last("LIMIT 100"))
                .stream()
                .map(this::toVO)
                .toList();
        // 返回订单列表。
        return Result.success(orders);
    }

    private OrderVO toVO(ServiceOrder o) {
        // 将订单实体转换为后台复用的订单 VO。
        return new OrderVO(o.getId(), o.getCommunityId(), o.getOrderNo(), o.getElderUserId(), o.getCreatorUserId(), o.getServiceItemId(), o.getServiceAddress(), o.getServiceStartTime(), o.getServiceEndTime(), o.getAssignMode(), o.getSpecifiedVolunteerUserId(), o.getAssignedVolunteerUserId(), o.getOrderStatus(), o.getRemark());
    }
}
