package com.eldercare.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.eldercare.api.client.CommunityFeignClient;
import com.eldercare.api.client.UserFeignClient;
import com.eldercare.api.client.VolunteerFeignClient;
import com.eldercare.api.dto.OrderCreateDTO;
import com.eldercare.api.dto.OrderEventDTO;
import com.eldercare.api.dto.VolunteerCheckAvailableDTO;
import com.eldercare.api.dto.VolunteerLockTimeDTO;
import com.eldercare.api.vo.OrderVO;
import com.eldercare.common.constant.MqConstants;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.context.UserInfoDTO;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.order.entity.OrderGrabRecord;
import com.eldercare.order.entity.OrderStatusLog;
import com.eldercare.order.entity.ServiceOrder;
import com.eldercare.order.mapper.OrderGrabRecordMapper;
import com.eldercare.order.mapper.OrderStatusLogMapper;
import com.eldercare.order.mapper.ServiceOrderMapper;
import com.eldercare.order.producer.OrderEventProducer;
import com.eldercare.order.service.OrderAppService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 订单业务实现。
 */
@Service
public class OrderAppServiceImpl implements OrderAppService {
    /**
     * 前端入参：指定志愿者模式。
     */
    private static final String ASSIGNED = "ASSIGNED";

    /**
     * 前端入参：公共订单池模式。
     */
    private static final String PUBLIC = "PUBLIC";

    /**
     * 数据库存储：指定志愿者模式。
     */
    private static final String DIRECT = "DIRECT";

    /**
     * 数据库存储：公共订单池模式。
     */
    private static final String PUBLIC_POOL = "PUBLIC_POOL";

    /**
     * 订单状态：待抢单。
     */
    private static final String WAIT_GRAB = "WAIT_GRAB";

    /**
     * 订单状态：已接单。
     */
    private static final String ACCEPTED = "ACCEPTED";

    /**
     * 订单状态：已取消。
     */
    private static final String CANCELLED = "CANCELLED";

    /**
     * 订单状态：已完成。
     */
    private static final String COMPLETED = "COMPLETED";

    /**
     * 公共池订单默认抢单超时时间，单位分钟。
     */
    private static final long DEFAULT_GRAB_TIMEOUT_MINUTES = 30L;

    /**
     * 订单 Mapper。
     */
    private final ServiceOrderMapper orderMapper;

    /**
     * 订单状态日志 Mapper。
     */
    private final OrderStatusLogMapper logMapper;

    /**
     * 抢单记录 Mapper。
     */
    private final OrderGrabRecordMapper grabMapper;

    /**
     * 用户服务 Feign，用于校验亲情号绑定关系。
     */
    private final UserFeignClient userFeignClient;

    /**
     * 社区服务 Feign，用于校验服务项目归属。
     */
    private final CommunityFeignClient communityFeignClient;

    /**
     * 志愿者服务 Feign，用于校验和锁定志愿者时间。
     */
    private final VolunteerFeignClient volunteerFeignClient;

    /**
     * Redisson 客户端，用于抢单分布式锁。
     */
    private final RedissonClient redissonClient;

    /**
     * 订单事件生产者。
     */
    private final OrderEventProducer producer;

    public OrderAppServiceImpl(ServiceOrderMapper orderMapper, OrderStatusLogMapper logMapper, OrderGrabRecordMapper grabMapper, UserFeignClient userFeignClient, CommunityFeignClient communityFeignClient, VolunteerFeignClient volunteerFeignClient, RedissonClient redissonClient, OrderEventProducer producer) {
        // 保存订单 Mapper。
        this.orderMapper = orderMapper;
        // 保存状态日志 Mapper。
        this.logMapper = logMapper;
        // 保存抢单记录 Mapper。
        this.grabMapper = grabMapper;
        // 保存用户服务 Feign。
        this.userFeignClient = userFeignClient;
        // 保存社区服务 Feign。
        this.communityFeignClient = communityFeignClient;
        // 保存志愿者服务 Feign。
        this.volunteerFeignClient = volunteerFeignClient;
        // 保存 Redisson 客户端。
        this.redissonClient = redissonClient;
        // 保存 MQ 生产者。
        this.producer = producer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(OrderCreateDTO dto) {
        // 从请求头读取当前登录用户和社区上下文。
        UserInfoDTO user = currentUser();
        // 调用 community-service 校验服务项目是否属于当前社区。
        if (Boolean.FALSE.equals(communityFeignClient.checkServiceItem(user.communityId(), dto.serviceItemId()).data())) {
            // 服务项目不存在或跨社区时终止下单。
            throw new BizException(ErrorCode.NOT_FOUND, "服务项目不属于当前社区");
        }
        // 老人角色下单时，只能给自己下单。
        if (UserContext.hasRole(RoleConstants.ELDER) && !user.userId().equals(dto.elderUserId())) {
            // 防止老人越权给其他老人下单。
            throw new BizException(ErrorCode.FORBIDDEN, "老人只能给自己下单");
        }
        // 亲情号角色下单时，如果不是给自己下单，就必须校验绑定关系。
        if (UserContext.hasRole(RoleConstants.FAMILY) && !user.userId().equals(dto.elderUserId())) {
            // 调用 user-service 内部接口校验亲情号是否绑定该老人。
            Boolean bind = userFeignClient.checkFamilyBind(user.communityId(), user.userId(), dto.elderUserId()).data();
            // 未绑定时禁止代下单。
            if (!Boolean.TRUE.equals(bind)) {
                // 抛出无权限异常。
                throw new BizException(ErrorCode.FORBIDDEN, "亲情号未绑定该老人");
            }
        }
        // 使用实体工厂方法创建订单，避免 Service 到处直接操作字段。
        ServiceOrder order = ServiceOrder.createBase(user.communityId(), nextOrderNo(), dto.elderUserId(), user.userId(),
                user.roles().isEmpty() ? null : user.roles().get(0), dto.serviceItemId(), dto.serviceAddress(),
                dto.serviceStartTime(), dto.serviceEndTime(), dto.remark());
        // 指定志愿者模式。
        if (ASSIGNED.equals(dto.assignMode())) {
            // 指定志愿者模式必须传志愿者 ID。
            if (dto.specifiedVolunteerUserId() == null)
                throw new BizException(ErrorCode.PARAM_ERROR, "指定志愿者不能为空");
            // 下单前校验志愿者在该服务时间段是否可用。
            Boolean ok = volunteerFeignClient.checkAvailable(new VolunteerCheckAvailableDTO(user.communityId(), dto.specifiedVolunteerUserId(), dto.serviceItemId(), dto.serviceStartTime(), dto.serviceEndTime())).data();
            // 志愿者不可用时禁止创建指定订单。
            if (!Boolean.TRUE.equals(ok)) throw new BizException(ErrorCode.VOLUNTEER_TIME_CONFLICT);
            // 指定志愿者订单的状态和志愿者字段由实体自身维护。
            order.assignToVolunteer(dto.specifiedVolunteerUserId(), DIRECT, ACCEPTED);
            // 先插入订单，生成订单 ID，供时间锁关联。
            orderMapper.insert(order);
            // 调用 volunteer-service 锁定志愿者时间。
            Boolean locked = volunteerFeignClient.lockTime(new VolunteerLockTimeDTO(user.communityId(), dto.specifiedVolunteerUserId(), order.getId(), dto.serviceItemId(), dto.serviceStartTime(), dto.serviceEndTime())).data();
            // 时间锁失败时回滚订单创建事务。
            if (!Boolean.TRUE.equals(locked)) throw new BizException(ErrorCode.VOLUNTEER_TIME_CONFLICT);
            // 记录订单状态日志。
            log(order, null, ACCEPTED, user.userId(), "CREATE");
        } else {
            // 公共池订单的派单模式和状态由实体自身维护。
            order.waitForGrab(PUBLIC_POOL, WAIT_GRAB, LocalDateTime.now().plusMinutes(DEFAULT_GRAB_TIMEOUT_MINUTES));
            // 插入订单。
            orderMapper.insert(order);
            // 记录创建日志。
            log(order, null, WAIT_GRAB, user.userId(), "CREATE");
        }
        // 发送订单创建 MQ 事件。
        send(order, "ORDER_CREATED", MqConstants.ORDER_CREATED_ROUTING_KEY, user.userId());
        // 返回订单 VO。
        return toVO(order);
    }

    @Override
    public List<OrderVO> myOrders() {
        // 读取当前用户上下文。
        UserInfoDTO user = currentUser();
        // 所有订单查询都必须限定 community_id。
        QueryWrapper<ServiceOrder> qw = new QueryWrapper<ServiceOrder>().eq("community_id", user.communityId()).eq("deleted", 0);
        // 志愿者查看自己接到的订单。
        if (UserContext.hasRole(RoleConstants.VOLUNTEER)) qw.eq("assigned_volunteer_user_id", user.userId());
            // 亲情号查看自己代发的订单。
        else if (UserContext.hasRole(RoleConstants.FAMILY)) qw.eq("creator_user_id", user.userId());
            // 老人查看自己的订单。
        else qw.eq("elder_user_id", user.userId());
        // 按最新订单倒序返回。
        return orderMapper.selectList(qw.orderByDesc("id")).stream().map(this::toVO).toList();
    }

    @Override
    public List<OrderVO> pool() {
        // 读取当前志愿者上下文。
        UserInfoDTO user = currentUser();
        // 查询本社区公共池待抢订单。
        return orderMapper.selectList(new QueryWrapper<ServiceOrder>()
                .eq("community_id", user.communityId())
                .eq("assign_mode", PUBLIC_POOL)
                .eq("order_status", WAIT_GRAB)
                .eq("deleted", 0)
                .orderByAsc("service_start_time")).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grab(Long orderId) {
        // 读取当前志愿者上下文。
        UserInfoDTO user = currentUser();
        // 抢单分布式锁 key，满足需求中的 lock:order:grab:{orderId}。
        RLock lock = redissonClient.getLock("lock:order:grab:" + orderId);
        try {
            // 最多等待 5 秒，锁自动过期 10 秒。
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) throw new BizException(ErrorCode.BUSINESS_CONFLICT, "抢单繁忙");
            // 锁内查询订单，限定本社区、待抢状态。
            ServiceOrder order = orderMapper.selectOne(new QueryWrapper<ServiceOrder>().eq("id", orderId)
                    .eq("community_id", user.communityId())
                    .eq("order_status", WAIT_GRAB)
                    .last("limit 1"));
            // 查不到说明订单不存在、跨社区或已被抢。
            if (order == null) throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "订单不可抢");
            // 抢单成功前先锁定志愿者时间。
            Boolean locked = volunteerFeignClient.lockTime(new VolunteerLockTimeDTO(user.communityId(), user.userId(), order.getId(), order.getServiceItemId(), order.getServiceStartTime(), order.getServiceEndTime())).data();
            // 志愿者时间冲突时终止抢单。
            if (!Boolean.TRUE.equals(locked)) throw new BizException(ErrorCode.VOLUNTEER_TIME_CONFLICT);
            // MySQL 条件更新，二次保证只有 WAIT_GRAB 状态能被抢。
            int updated = orderMapper.update(null, new UpdateWrapper<ServiceOrder>()
                    // 设置接单志愿者。
                    .set("assigned_volunteer_user_id", user.userId())
                    // 设置订单状态为已接单。
                    .set("order_status", ACCEPTED)
                    // 设置接单时间。
                    .set("assigned_at", LocalDateTime.now())
                    // 乐观锁版本号递增。
                    .setSql("version = version + 1")
                    // 限定订单 ID。
                    .eq("id", orderId)
                    // 限定社区 ID。
                    .eq("community_id", user.communityId())
                    // 核心并发条件：只有待抢单状态可以更新成功。
                    .eq("order_status", WAIT_GRAB));
            // 影响行数不是 1，说明订单已被其他人抢走。
            if (updated != 1) {
                // 条件更新失败时释放刚刚锁定的志愿者时间，避免脏占用。
                volunteerFeignClient.releaseTime(new VolunteerLockTimeDTO(user.communityId(), user.userId(), order.getId(), order.getServiceItemId(), order.getServiceStartTime(), order.getServiceEndTime()));
                // 抛出状态异常。
                throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "订单已被抢");
            }
            // 更新内存对象，便于后续写日志和发 MQ。
            order.markGrabbed(user.userId(), ACCEPTED);
            // 记录抢单流水。
            OrderGrabRecord r = OrderGrabRecord.success(order, user.userId(), UUID.randomUUID().toString());
            // 插入抢单记录。
            grabMapper.insert(r);
            // 写订单状态日志。
            log(order, WAIT_GRAB, ACCEPTED, user.userId(), "GRAB");
            // 发送抢单成功 MQ 事件。
            send(order, "ORDER_GRABBED", MqConstants.ORDER_GRABBED_ROUTING_KEY, user.userId());
        } catch (InterruptedException e) {
            // 恢复线程中断标记。
            Thread.currentThread().interrupt();
            // 转换为业务异常。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "抢单锁获取失败");
        } finally {
            // 当前线程持有锁时释放。
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId) {
        // 取消订单并发送取消事件。
        changeStatus(orderId, CANCELLED, "CANCEL", MqConstants.ORDER_CANCELLED_ROUTING_KEY, "ORDER_CANCELLED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long orderId) {
        // 完成订单并发送完成事件。
        changeStatus(orderId, COMPLETED, "COMPLETE", MqConstants.ORDER_COMPLETED_ROUTING_KEY, "ORDER_COMPLETED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoCloseTimeoutOrders() {
        // 当前时间作为超时判断基准。
        LocalDateTime now = LocalDateTime.now();
        // 扫描待抢单且已超过抢单截止时间，或预约开始时间已经过去的订单。
        List<ServiceOrder> timeoutOrders = orderMapper.selectList(new QueryWrapper<ServiceOrder>()
                .eq("order_status", WAIT_GRAB)
                .eq("deleted", 0)
                .and(wrapper -> wrapper.le("grab_deadline", now).or().le("service_start_time", now))
                .last("LIMIT 50"));
        // 逐条执行条件关闭，避免一次大事务锁太多行。
        int closed = 0;
        // 遍历超时订单。
        for (ServiceOrder order : timeoutOrders) {
            // 条件更新保证只有仍处于 WAIT_GRAB 的订单会被自动关闭。
            int updated = orderMapper.update(null, new UpdateWrapper<ServiceOrder>()
                    .set("order_status", CANCELLED)
                    .set("cancel_reason", "系统超时自动关闭")
                    .set("cancelled_at", now)
                    .setSql("version = version + 1")
                    .eq("id", order.getId())
                    .eq("community_id", order.getCommunityId())
                    .eq("order_status", WAIT_GRAB));
            // 更新成功才写日志和通知。
            if (updated == 1) {
                // 更新内存状态。
                order.markStatusChanged(CANCELLED);
                // 记录系统自动关闭日志，operator 使用 0 表示系统。
                log(order, WAIT_GRAB, CANCELLED, 0L, "AUTO_TIMEOUT_CANCEL");
                // 发送自动关闭通知。
                send(order, "ORDER_CANCELLED", MqConstants.ORDER_CANCELLED_ROUTING_KEY, 0L);
                // 累加关闭数量。
                closed++;
            }
        }
        // 返回本次关闭数量。
        return closed;
    }

    private void changeStatus(Long orderId, String to, String op, String routing, String event) {
        // 读取当前用户上下文。
        UserInfoDTO user = currentUser();
        // 查询本社区订单。
        ServiceOrder order = orderMapper.selectOne(new QueryWrapper<ServiceOrder>().eq("id", orderId).eq("community_id", user.communityId()).eq("deleted", 0).last("limit 1"));
        // 订单不存在时直接报错。
        if (order == null) throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        // 校验当前用户是否有权操作该订单。
        checkOperatePermission(order, to, user);
        // 保存原状态，用于写状态日志。
        String from = order.getOrderStatus();
        // 校验订单当前状态是否允许流转到目标状态。
        checkStatusChange(from, to);
        // 条件更新状态，并递增版本号。
        int updated = orderMapper.update(null, new UpdateWrapper<ServiceOrder>()
                // 写入目标状态。
                .set("order_status", to)
                // 取消订单时写入取消时间。
                .set(CANCELLED.equals(to), "cancelled_at", LocalDateTime.now())
                // 完成订单时写入完成时间。
                .set(COMPLETED.equals(to), "completed_at", LocalDateTime.now())
                // 乐观锁版本号递增。
                .setSql("version = version + 1")
                // 限定订单 ID。
                .eq("id", orderId)
                // 限定社区 ID。
                .eq("community_id", user.communityId())
                // 限定原状态，防止并发状态流转覆盖。
                .eq("order_status", from));
        // 更新失败说明状态已经被其他请求修改。
        if (updated != 1) throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "订单状态已变化");
        // 取消已接单订单时释放志愿者时间。
        if (CANCELLED.equals(to) && order.getAssignedVolunteerUserId() != null) {
            // 通知 volunteer-service 释放该订单占用的志愿者时间锁。
            volunteerFeignClient.releaseTime(new VolunteerLockTimeDTO(user.communityId(), order.getAssignedVolunteerUserId(), order.getId(), order.getServiceItemId(), order.getServiceStartTime(), order.getServiceEndTime()));
        }
        // 更新内存状态。
        order.markStatusChanged(to);
        // 写状态流转日志。
        log(order, from, to, user.userId(), op);
        // 发送状态变更 MQ 事件。
        send(order, event, routing, user.userId());
    }

    /**
     * 校验当前用户是否有权执行订单状态操作。
     *
     * @param order 目标订单。
     * @param to    目标状态。
     * @param user  当前登录用户。
     */
    private void checkOperatePermission(ServiceOrder order, String to, UserInfoDTO user) {
        // 取消订单只能由老人本人或代发亲情号操作。
        if (CANCELLED.equals(to)) {
            // 老人本人可以取消自己的订单。
            boolean elderOwner = UserContext.hasRole(RoleConstants.ELDER) && user.userId().equals(order.getElderUserId());
            // 亲情号只能取消自己创建的代发订单。
            boolean familyCreator = UserContext.hasRole(RoleConstants.FAMILY) && user.userId().equals(order.getCreatorUserId());
            // 两种条件都不满足时禁止取消。
            if (!elderOwner && !familyCreator) {
                // 抛出无权限异常。
                throw new BizException(ErrorCode.FORBIDDEN, "只能取消自己创建或自己的老人订单");
            }
        }
        // 完成订单只能由实际接单志愿者操作。
        if (COMPLETED.equals(to)) {
            // 当前用户必须是志愿者并且等于订单接单志愿者。
            boolean assignedVolunteer = UserContext.hasRole(RoleConstants.VOLUNTEER) && user.userId().equals(order.getAssignedVolunteerUserId());
            // 不匹配时禁止完成。
            if (!assignedVolunteer) {
                // 抛出无权限异常。
                throw new BizException(ErrorCode.FORBIDDEN, "只能完成自己接到的订单");
            }
        }
    }

    /**
     * 校验订单状态是否允许流转。
     *
     * @param from 当前状态。
     * @param to   目标状态。
     */
    private void checkStatusChange(String from, String to) {
        // 已取消和已完成都是终态，不能再流转。
        if (CANCELLED.equals(from) || COMPLETED.equals(from)) {
            // 抛出状态错误。
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "终态订单不能继续操作");
        }
        // 完成订单只能从已接单状态进入。
        if (COMPLETED.equals(to) && !ACCEPTED.equals(from)) {
            // 待抢单订单不能直接完成。
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "只有已接单订单才能完成");
        }
        // 取消订单允许从待抢单或已接单进入。
        if (CANCELLED.equals(to) && !(WAIT_GRAB.equals(from) || ACCEPTED.equals(from))) {
            // 其他状态不允许取消。
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "当前状态不能取消");
        }
    }

    private UserInfoDTO currentUser() {
        // 从请求头解析用户上下文。
        UserContext.loadFromCurrentRequest();
        // 校验社区上下文存在。
        UserContext.requireCommunityId();
        // 返回当前用户上下文。
        return UserContext.get();
    }

    private void log(ServiceOrder order, String from, String to, Long operator, String type) {
        // 通过实体工厂方法构造状态日志，减少 Service 对日志字段的直接拼装。
        OrderStatusLog log = OrderStatusLog.of(order, from, to, operator, type);
        // 插入状态日志。
        logMapper.insert(log);
    }

    private void send(ServiceOrder order, String event, String routing, Long operator) {
        // 构造订单事件并发送到 RabbitMQ。
        producer.send(routing, new OrderEventDTO(UUID.randomUUID().toString(), event, order.getCommunityId(), order.getId(), order.getOrderNo(), order.getElderUserId(), operator, "订单事件：" + event, LocalDateTime.now()));
    }

    private OrderVO toVO(ServiceOrder o) {
        // 将订单实体转换为接口返回对象。
        return new OrderVO(o.getId(), o.getCommunityId(), o.getOrderNo(), o.getElderUserId(), o.getCreatorUserId(), o.getServiceItemId(), o.getServiceAddress(), o.getServiceStartTime(), o.getServiceEndTime(), o.getAssignMode(), o.getSpecifiedVolunteerUserId(), o.getAssignedVolunteerUserId(), o.getOrderStatus(), o.getRemark());
    }

    private String nextOrderNo() {
        // 订单号生成集中在单独方法，后续替换成雪花算法或号段服务时只改这里。
        return "EC" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }
}
