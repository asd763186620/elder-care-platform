package com.eldercare.order.controller;

import com.eldercare.api.dto.OrderCreateDTO;
import com.eldercare.api.vo.OrderVO;
import com.eldercare.common.annotation.RepeatSubmit;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.response.Result;
import com.eldercare.order.service.OrderAppService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约单业务接口。
 * 说明：网关已经完成 JWT 校验，下游服务通过 UserContext 获取当前用户和社区。
 */
@RestController
@RequestMapping("/orders")
public class OrderController {
    /** 订单业务服务，封装下单、抢单、取消和完成等核心规则。 */
    private final OrderAppService service;

    /**
     * 构造器注入订单业务服务。
     *
     * @param service 订单业务服务。
     */
    public OrderController(OrderAppService service) {
        this.service = service;
    }

    /**
     * 发布预约单。
     * 支持 ASSIGNED 指定志愿者和 PUBLIC 公共订单池两种模式。
     *
     * @param dto 下单参数。
     * @return 创建后的订单展示对象。
     */
    @PostMapping
    @RepeatSubmit(expireSeconds = 5)
    @RequireRole({RoleConstants.ELDER, RoleConstants.FAMILY})
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    /**
     * 查询当前登录用户相关订单。
     * 老人看自己的订单，亲情号看自己代发的订单，志愿者看自己接到的订单。
     *
     * @return 我的订单列表。
     */
    @GetMapping("/my")
    public Result<List<OrderVO>> my() {
        return Result.success(service.myOrders());
    }

    /**
     * 查询公共订单池。
     * 只有当前社区的志愿者可以看到本社区 WAIT_GRAB 状态的公共订单。
     *
     * @return 公共订单池列表。
     */
    @GetMapping("/pool")
    @RequireRole(RoleConstants.VOLUNTEER)
    public Result<List<OrderVO>> pool() {
        return Result.success(service.pool());
    }

    /**
     * 志愿者抢单。
     * 内部使用 Redisson 分布式锁和 MySQL 条件更新保证并发安全。
     *
     * @param orderId 订单 ID。
     * @return 空结果，成功时 code 为 0。
     */
    @PostMapping("/{orderId}/grab")
    @RepeatSubmit(expireSeconds = 5)
    @RequireRole(RoleConstants.VOLUNTEER)
    public Result<Void> grab(@PathVariable Long orderId) {
        service.grab(orderId);
        return Result.success();
    }

    /**
     * 取消订单。
     * 老人或亲情号只能取消自己有权限操作的订单。
     *
     * @param orderId 订单 ID。
     * @return 空结果，成功时 code 为 0。
     */
    @PostMapping("/{orderId}/cancel")
    @RepeatSubmit(expireSeconds = 5)
    @RequireRole({RoleConstants.ELDER, RoleConstants.FAMILY})
    public Result<Void> cancel(@PathVariable Long orderId) {
        service.cancel(orderId);
        return Result.success();
    }

    /**
     * 完成订单。
     * 第一版由接单志愿者调用完成接口。
     *
     * @param orderId 订单 ID。
     * @return 空结果，成功时 code 为 0。
     */
    @PostMapping("/{orderId}/complete")
    @RepeatSubmit(expireSeconds = 5)
    @RequireRole(RoleConstants.VOLUNTEER)
    public Result<Void> complete(@PathVariable Long orderId) {
        service.complete(orderId);
        return Result.success();
    }
}
