package com.eldercare.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.eldercare.api.client.CommunityFeignClient;
import com.eldercare.api.client.UserFeignClient;
import com.eldercare.api.client.VolunteerFeignClient;
import com.eldercare.api.dto.*;
import com.eldercare.api.vo.*;
import com.eldercare.common.constant.MqConstants;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.context.UserInfoDTO;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.order.entity.OrderEvaluation;
import com.eldercare.order.entity.OrderEventOutbox;
import com.eldercare.order.entity.OrderGrabRecord;
import com.eldercare.order.entity.OrderStatusLog;
import com.eldercare.order.entity.ServiceOrder;
import com.eldercare.order.enums.OrderStatusEnum;
import com.eldercare.order.mapper.OrderEvaluationMapper;
import com.eldercare.order.mapper.OrderEventOutboxMapper;
import com.eldercare.order.mapper.OrderGrabRecordMapper;
import com.eldercare.order.mapper.OrderStatusLogMapper;
import com.eldercare.order.mapper.ServiceOrderMapper;
import com.eldercare.order.producer.OrderEventProducer;
import com.eldercare.order.service.OrderAppService;
import com.eldercare.order.util.OrderNoGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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

    /** 订单状态：待抢单。 */
    private static final String WAIT_GRAB = OrderStatusEnum.WAIT_GRAB.code();
    /** 订单状态：待服务。 */
    private static final String WAIT_SERVICE = OrderStatusEnum.WAIT_SERVICE.code();
    /** 订单状态：服务中。 */
    private static final String IN_SERVICE = OrderStatusEnum.IN_SERVICE.code();
    /** 订单状态：待确认。 */
    private static final String WAIT_CONFIRM = OrderStatusEnum.WAIT_CONFIRM.code();
    /** 订单状态：已取消。 */
    private static final String CANCELLED = OrderStatusEnum.CANCELLED.code();
    /** 订单状态：超时关闭。 */
    private static final String TIMEOUT_CLOSED = OrderStatusEnum.TIMEOUT_CLOSED.code();
    /** 订单状态：已完成。 */
    private static final String COMPLETED = OrderStatusEnum.COMPLETED.code();

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
     * 订单评价 Mapper。
     */
    private final OrderEvaluationMapper evaluationMapper;

    /**
     * Outbox 本地消息 Mapper。
     */
    private final OrderEventOutboxMapper outboxMapper;

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

    /**
     * 订单号生成器。
     */
    private final OrderNoGenerator orderNoGenerator;

    /**
     * JSON 序列化器，用于写入 Outbox payload。
     */
    private final ObjectMapper objectMapper;

    public OrderAppServiceImpl(ServiceOrderMapper orderMapper,
                               OrderStatusLogMapper logMapper,
                               OrderGrabRecordMapper grabMapper,
                               OrderEvaluationMapper evaluationMapper,
                               OrderEventOutboxMapper outboxMapper,
                               UserFeignClient userFeignClient,
                               CommunityFeignClient communityFeignClient,
                               VolunteerFeignClient volunteerFeignClient,
                               RedissonClient redissonClient,
                               OrderEventProducer producer,
                               OrderNoGenerator orderNoGenerator,
                               ObjectMapper objectMapper) {
        // 保存订单 Mapper。
        this.orderMapper = orderMapper;
        // 保存状态日志 Mapper。
        this.logMapper = logMapper;
        // 保存抢单记录 Mapper。
        this.grabMapper = grabMapper;
        // 保存评价 Mapper。
        this.evaluationMapper = evaluationMapper;
        // 保存 Outbox Mapper。
        this.outboxMapper = outboxMapper;
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
        // 保存订单号生成器。
        this.orderNoGenerator = orderNoGenerator;
        // 保存 JSON 序列化器。
        this.objectMapper = objectMapper;
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
            order.assignToVolunteer(dto.specifiedVolunteerUserId(), DIRECT, WAIT_SERVICE);
            // 先插入订单，生成订单 ID，供时间锁关联。
            orderMapper.insert(order);
            // 调用 volunteer-service 锁定志愿者时间。
            Boolean locked = volunteerFeignClient.lockTime(new VolunteerLockTimeDTO(user.communityId(), dto.specifiedVolunteerUserId(), order.getId(), dto.serviceItemId(), dto.serviceStartTime(), dto.serviceEndTime())).data();
            // 时间锁失败时回滚订单创建事务。
            if (!Boolean.TRUE.equals(locked)) throw new BizException(ErrorCode.VOLUNTEER_TIME_CONFLICT);
            // 记录订单状态日志。
            log(order, null, WAIT_SERVICE, user.userId(), "CREATE");
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
    public OrderDetailVO detail(Long orderId) {
        // 读取当前用户上下文。
        UserInfoDTO user = currentUser();
        // 查询本社区订单。
        ServiceOrder order = selectOrderForRead(orderId, user);
        // 按小程序身份校验详情访问权限。
        checkReadPermission(order, user);
        // 查询状态日志。
        List<OrderDetailVO.StatusLogItem> logs = logMapper.selectList(new QueryWrapper<OrderStatusLog>()
                        .eq("community_id", user.communityId())
                        .eq("order_id", orderId)
                        .orderByAsc("id"))
                .stream()
                .map(log -> new OrderDetailVO.StatusLogItem(log.getFromStatus(), log.getToStatus(), log.getOperatorUserId(),
                        log.getOperatorRole(), log.getOperateType(), log.getOperateRemark(), log.getCreatedAt()))
                .toList();
        // 第一版详情先返回订单和日志，跨服务资料后续可继续丰富。
        return new OrderDetailVO(toVO(order), null, null, familyUserId(order), volunteerBrief(order), logs);
    }

    @Override
    public CursorPageVO<OrderVO> myOrdersPage(String status, Long lastId, Integer size) {
        // 读取当前用户上下文。
        UserInfoDTO user = currentUser();
        // 构造当前身份对应的基础查询。
        QueryWrapper<ServiceOrder> qw = buildMyOrderQuery(user);
        // 状态可选过滤。
        if (status != null && !status.isBlank()) {
            // 按状态过滤。
            qw.eq("order_status", status);
        }
        // 游标分页条件。
        applyCursor(qw, lastId, normalizeSize(size));
        // 查询一页数据。
        List<OrderVO> records = orderMapper.selectList(qw).stream().map(this::toVO).toList();
        // 返回游标分页对象。
        return page(records, normalizeSize(size), OrderVO::id);
    }

    @Override
    public CursorPageVO<OrderVO> poolPage(Long serviceItemId, Long lastId, Integer size) {
        // 读取当前志愿者上下文。
        UserInfoDTO user = currentUser();
        // 构造公共池查询，只看本社区、待抢单、未过服务开始时间的订单。
        QueryWrapper<ServiceOrder> qw = new QueryWrapper<ServiceOrder>()
                .eq("community_id", user.communityId())
                .eq("assign_mode", PUBLIC_POOL)
                .eq("order_status", WAIT_GRAB)
                .gt("service_start_time", LocalDateTime.now())
                .eq("deleted", 0);
        // 服务项目可选过滤。
        if (serviceItemId != null) {
            // 按服务项目筛选公共池。
            qw.eq("service_item_id", serviceItemId);
        }
        // 应用游标分页。
        applyCursor(qw, lastId, normalizeSize(size));
        // 查询分页数据。
        List<OrderVO> records = orderMapper.selectList(qw).stream().map(this::toVO).toList();
        // 返回分页结果。
        return page(records, normalizeSize(size), OrderVO::id);
    }

    @Override
    public OrderStatusCountVO statusCount() {
        // 读取当前用户上下文。
        UserInfoDTO user = currentUser();
        // 基础查询限定当前身份可见订单。
        QueryWrapper<ServiceOrder> base = buildMyOrderQuery(user);
        // 分别统计小程序 tab 所需状态数量。
        return new OrderStatusCountVO(countByStatus(base, WAIT_GRAB), countByStatus(base, WAIT_SERVICE),
                countByStatus(base, IN_SERVICE), countByStatus(base, WAIT_CONFIRM), countByStatus(base, COMPLETED),
                countByStatus(base, CANCELLED) + countByStatus(base, TIMEOUT_CLOSED));
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
                    // 设置订单状态为待服务。
                    .set("order_status", WAIT_SERVICE)
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
            order.markGrabbed(user.userId(), WAIT_SERVICE);
            // 记录抢单流水。
            OrderGrabRecord r = OrderGrabRecord.success(order, user.userId(), UUID.randomUUID().toString());
            // 插入抢单记录。
            grabMapper.insert(r);
            // 写订单状态日志。
            log(order, WAIT_GRAB, WAIT_SERVICE, user.userId(), "GRAB");
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
        changeStatus(orderId, OrderStatusEnum.CANCELLED, "CANCEL", MqConstants.ORDER_CANCELLED_ROUTING_KEY, "ORDER_CANCELLED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long orderId) {
        // 先加载用户上下文，避免直接读取 ThreadLocal 时为空。
        currentUser();
        // 兼容旧接口：志愿者调用 complete 时按“提交完成”处理。
        if (UserContext.hasRole(RoleConstants.VOLUNTEER)) {
            // 志愿者提交完成，等待老人或亲情号确认。
            submitComplete(orderId);
            // 结束兼容处理。
            return;
        }
        // 老人或亲情号调用旧 complete 时按“确认完成”处理。
        confirm(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long orderId) {
        // 开始服务并发送开始事件。
        changeStatus(orderId, OrderStatusEnum.IN_SERVICE, "START", MqConstants.ORDER_STARTED_ROUTING_KEY, "ORDER_STARTED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitComplete(Long orderId) {
        // 志愿者提交完成，进入待确认。
        changeStatus(orderId, OrderStatusEnum.WAIT_CONFIRM, "SUBMIT_COMPLETE", MqConstants.ORDER_SUBMITTED_ROUTING_KEY, "ORDER_SUBMITTED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) {
        // 老人或亲情号确认完成。
        changeStatus(orderId, OrderStatusEnum.COMPLETED, "CONFIRM_COMPLETE", MqConstants.ORDER_COMPLETED_ROUTING_KEY, "ORDER_COMPLETED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluate(Long orderId, OrderEvaluateDTO dto) {
        // 读取当前用户上下文。
        UserInfoDTO user = currentUser();
        // 查询本社区订单。
        ServiceOrder order = selectOrderForRead(orderId, user);
        // 只有老人本人或已绑定亲情号可以评价。
        checkConfirmPermission(order, user);
        // 只有已完成订单可以评价。
        if (!COMPLETED.equals(order.getOrderStatus())) {
            // 状态不正确时拒绝评价。
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "订单完成后才能评价");
        }
        // 订单没有志愿者时不能评价。
        if (order.getAssignedVolunteerUserId() == null) {
            // 数据异常时拒绝评价。
            throw new BizException(ErrorCode.PARAM_ERROR, "订单没有接单志愿者");
        }
        try {
            // 插入评价记录，uk_order_id 保证一个订单只能评价一次。
            evaluationMapper.insert(OrderEvaluation.of(order, user.userId(), dto.score(), dto.tags(), dto.content(), dto.anonymous()));
        } catch (DuplicateKeyException exception) {
            // 唯一索引冲突说明已经评价过。
            throw new BizException(ErrorCode.DATA_EXISTS, "该订单已评价");
        }
    }

    @Override
    public CursorPageVO<OrderEvaluationVO> volunteerReviews(Long volunteerId, Long lastId, Integer size) {
        // 读取当前用户上下文用于社区隔离。
        UserInfoDTO user = currentUser();
        // 构造评价分页查询。
        QueryWrapper<OrderEvaluation> qw = new QueryWrapper<OrderEvaluation>()
                .eq("community_id", user.communityId())
                .eq("volunteer_user_id", volunteerId);
        // 应用游标分页。
        applyCursor(qw, lastId, normalizeSize(size));
        // 查询并转换评价。
        List<OrderEvaluationVO> records = evaluationMapper.selectList(qw).stream()
                .map(e -> new OrderEvaluationVO(e.getId(), e.getOrderId(), e.getVolunteerUserId(), e.getScore(), e.getTags(), e.getContent(), e.getAnonymous(), e.getCreateTime()))
                .toList();
        // 返回分页结果。
        return page(records, normalizeSize(size), OrderEvaluationVO::id);
    }

    @Override
    public VolunteerScoreVO volunteerScore(Long volunteerId) {
        // 读取当前用户上下文用于社区隔离。
        UserInfoDTO user = currentUser();
        // 查询该志愿者所有评价。
        List<OrderEvaluation> evaluations = evaluationMapper.selectList(new QueryWrapper<OrderEvaluation>()
                .eq("community_id", user.communityId())
                .eq("volunteer_user_id", volunteerId));
        // 评价数量。
        long evaluationCount = evaluations.size();
        // 平均分，未评价时默认 0。
        int avgScore = evaluationCount == 0 ? 0 : (int) Math.round(evaluations.stream().mapToInt(OrderEvaluation::getScore).average().orElse(0));
        // 完成订单数量。
        Long totalServiceCount = orderMapper.selectCount(new QueryWrapper<ServiceOrder>()
                .eq("community_id", user.communityId())
                .eq("assigned_volunteer_user_id", volunteerId)
                .eq("order_status", COMPLETED)
                .eq("deleted", 0));
        // 返回评分信息。
        return new VolunteerScoreVO(volunteerId, avgScore, totalServiceCount == null ? 0 : totalServiceCount, evaluationCount);
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
                    .set("order_status", TIMEOUT_CLOSED)
                    .set("cancel_reason", "系统超时自动关闭")
                    .set("cancelled_at", now)
                    .setSql("version = version + 1")
                    .eq("id", order.getId())
                    .eq("community_id", order.getCommunityId())
                    .eq("order_status", WAIT_GRAB));
            // 更新成功才写日志和通知。
            if (updated == 1) {
                // 更新内存状态。
                order.markStatusChanged(TIMEOUT_CLOSED);
                // 记录系统自动关闭日志，operator 使用 0 表示系统。
                log(order, WAIT_GRAB, TIMEOUT_CLOSED, 0L, "AUTO_TIMEOUT_CANCEL");
                // 发送自动关闭通知。
                send(order, "ORDER_CANCELLED", MqConstants.ORDER_CANCELLED_ROUTING_KEY, 0L);
                // 累加关闭数量。
                closed++;
            }
        }
        // 返回本次关闭数量。
        return closed;
    }

    private void changeStatus(Long orderId, OrderStatusEnum toStatus, String op, String routing, String event) {
        // 读取当前用户上下文。
        UserInfoDTO user = currentUser();
        // 查询本社区订单。
        ServiceOrder order = orderMapper.selectOne(new QueryWrapper<ServiceOrder>().eq("id", orderId).eq("community_id", user.communityId()).eq("deleted", 0).last("limit 1"));
        // 订单不存在时直接报错。
        if (order == null) throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        // 校验当前用户是否有权操作该订单。
        checkOperatePermission(order, toStatus, user);
        // 保存原状态，用于写状态日志。
        String from = order.getOrderStatus();
        // 校验订单当前状态是否允许流转到目标状态。
        OrderStatusEnum.checkCanTransit(from, toStatus);
        // 目标状态字符串。
        String to = toStatus.code();
        // 条件更新状态，并递增版本号。
        int updated = orderMapper.update(null, new UpdateWrapper<ServiceOrder>()
                // 写入目标状态。
                .set("order_status", to)
                // 取消订单时写入取消时间。
                .set(CANCELLED.equals(to), "cancelled_at", LocalDateTime.now())
                // 开始服务时写入开始服务时间。
                .set(IN_SERVICE.equals(to), "service_started_at", LocalDateTime.now())
                // 提交完成时写入提交完成时间。
                .set(WAIT_CONFIRM.equals(to), "submitted_at", LocalDateTime.now())
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
    private void checkOperatePermission(ServiceOrder order, OrderStatusEnum toStatus, UserInfoDTO user) {
        // 目标状态编码。
        String to = toStatus.code();
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
        // 开始服务和提交完成只能由接单志愿者操作。
        if (IN_SERVICE.equals(to) || WAIT_CONFIRM.equals(to)) {
            // 校验接单志愿者。
            checkAssignedVolunteer(order, user);
        }
        // 确认完成只能由老人本人或已绑定亲情号操作。
        if (COMPLETED.equals(to)) {
            // 校验确认权限。
            checkConfirmPermission(order, user);
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
        // 生成事件 ID，生产端和消费端都用它做幂等。
        String eventId = UUID.randomUUID().toString();
        // 构造订单事件。
        OrderEventDTO dto = new OrderEventDTO(eventId, event, order.getCommunityId(), order.getId(), order.getOrderNo(), order.getElderUserId(), operator, "订单事件：" + event, LocalDateTime.now());
        try {
            // 订单事务内只写 Outbox，不直接阻塞发送 MQ。
            String payload = objectMapper.writeValueAsString(dto);
            // 插入本地消息表，事务提交后由定时任务发送。
            outboxMapper.insert(OrderEventOutbox.init(eventId, order.getId(), event, MqConstants.ORDER_EVENT_EXCHANGE, routing, payload));
        } catch (JsonProcessingException exception) {
            // 序列化失败说明代码或字段异常，必须回滚当前业务事务。
            throw new BizException(ErrorCode.SYSTEM_ERROR, "订单事件序列化失败");
        }
    }

    private OrderVO toVO(ServiceOrder o) {
        // 将订单实体转换为接口返回对象。
        return new OrderVO(o.getId(), o.getCommunityId(), o.getOrderNo(), o.getElderUserId(), o.getCreatorUserId(), o.getServiceItemId(), o.getServiceAddress(), o.getServiceStartTime(), o.getServiceEndTime(), o.getAssignMode(), o.getSpecifiedVolunteerUserId(), o.getAssignedVolunteerUserId(), o.getOrderStatus(), o.getRemark());
    }

    private String nextOrderNo() {
        // 使用订单号生成器创建 EC+日期+雪花 ID 格式订单号。
        return orderNoGenerator.nextOrderNo();
    }

    /**
     * 查询当前社区订单。
     */
    private ServiceOrder selectOrderForRead(Long orderId, UserInfoDTO user) {
        // 使用 id + community_id 查询，防止跨社区读取。
        ServiceOrder order = orderMapper.selectOne(new QueryWrapper<ServiceOrder>()
                .eq("id", orderId)
                .eq("community_id", user.communityId())
                .eq("deleted", 0)
                .last("limit 1"));
        // 不存在时抛出资源不存在。
        if (order == null) {
            // 抛出不存在异常。
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        // 返回订单。
        return order;
    }

    /**
     * 校验订单读取权限。
     */
    private void checkReadPermission(ServiceOrder order, UserInfoDTO user) {
        // 老人本人可以看自己的订单。
        boolean elderOwner = UserContext.hasRole(RoleConstants.ELDER) && user.userId().equals(order.getElderUserId());
        // 亲情号自己代发可以看。
        boolean familyCreator = UserContext.hasRole(RoleConstants.FAMILY) && user.userId().equals(order.getCreatorUserId());
        // 亲情号绑定老人也可以看。
        boolean familyBound = UserContext.hasRole(RoleConstants.FAMILY) && Boolean.TRUE.equals(userFeignClient.checkFamilyBind(user.communityId(), user.userId(), order.getElderUserId()).data());
        // 志愿者接单后可以看。
        boolean assignedVolunteer = UserContext.hasRole(RoleConstants.VOLUNTEER) && user.userId().equals(order.getAssignedVolunteerUserId());
        // 志愿者可以查看公共池待抢订单。
        boolean publicPool = UserContext.hasRole(RoleConstants.VOLUNTEER) && PUBLIC_POOL.equals(order.getAssignMode()) && WAIT_GRAB.equals(order.getOrderStatus());
        // 所有条件都不满足时拒绝。
        if (!elderOwner && !familyCreator && !familyBound && !assignedVolunteer && !publicPool) {
            // 抛出无权限异常。
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该订单");
        }
    }

    /**
     * 校验当前用户是接单志愿者。
     */
    private void checkAssignedVolunteer(ServiceOrder order, UserInfoDTO user) {
        // 当前用户必须是志愿者并且等于订单接单志愿者。
        boolean assignedVolunteer = UserContext.hasRole(RoleConstants.VOLUNTEER) && user.userId().equals(order.getAssignedVolunteerUserId());
        // 不匹配时禁止操作。
        if (!assignedVolunteer) {
            // 抛出无权限异常。
            throw new BizException(ErrorCode.FORBIDDEN, "只能操作自己接到的订单");
        }
    }

    /**
     * 校验老人或亲情号确认权限。
     */
    private void checkConfirmPermission(ServiceOrder order, UserInfoDTO user) {
        // 老人本人可以确认。
        boolean elderOwner = UserContext.hasRole(RoleConstants.ELDER) && user.userId().equals(order.getElderUserId());
        // 亲情号创建人可以确认。
        boolean familyCreator = UserContext.hasRole(RoleConstants.FAMILY) && user.userId().equals(order.getCreatorUserId());
        // 已绑定亲情号可以确认。
        boolean familyBound = UserContext.hasRole(RoleConstants.FAMILY) && Boolean.TRUE.equals(userFeignClient.checkFamilyBind(user.communityId(), user.userId(), order.getElderUserId()).data());
        // 三种条件都不满足时禁止确认。
        if (!elderOwner && !familyCreator && !familyBound) {
            // 抛出无权限异常。
            throw new BizException(ErrorCode.FORBIDDEN, "无权确认该订单");
        }
    }

    /**
     * 构造我的订单基础查询。
     */
    private QueryWrapper<ServiceOrder> buildMyOrderQuery(UserInfoDTO user) {
        // 所有订单查询都必须限定 community_id。
        QueryWrapper<ServiceOrder> qw = new QueryWrapper<ServiceOrder>().eq("community_id", user.communityId()).eq("deleted", 0);
        // 志愿者查看自己接到的订单。
        if (UserContext.hasRole(RoleConstants.VOLUNTEER)) {
            // 按接单志愿者过滤。
            qw.eq("assigned_volunteer_user_id", user.userId());
        } else if (UserContext.hasRole(RoleConstants.FAMILY)) {
            // 亲情号查看自己代发的订单。
            qw.eq("creator_user_id", user.userId());
        } else {
            // 老人查看自己的订单。
            qw.eq("elder_user_id", user.userId());
        }
        // 返回基础查询。
        return qw;
    }

    /**
     * 统计指定状态数量。
     */
    private long countByStatus(QueryWrapper<ServiceOrder> base, String status) {
        // MyBatis-Plus QueryWrapper 是可变对象，这里复制 SQL 条件需要重新拼一个 wrapper。
        UserInfoDTO user = currentUser();
        // 重新构造当前身份基础查询，避免复用 wrapper 造成条件叠加。
        QueryWrapper<ServiceOrder> qw = buildMyOrderQuery(user).eq("order_status", status);
        // 查询数量。
        Long count = orderMapper.selectCount(qw);
        // 空值按 0 处理。
        return count == null ? 0L : count;
    }

    /**
     * 应用游标分页条件。
     */
    private void applyCursor(QueryWrapper<?> qw, Long lastId, int size) {
        // 传了 lastId 时只查更早记录。
        if (lastId != null) {
            // 添加 id < lastId 条件。
            qw.lt("id", lastId);
        }
        // 按 ID 倒序，限制 size + 1 判断是否还有更多。
        qw.orderByDesc("id").last("limit " + (size + 1));
    }

    /**
     * 规整分页大小。
     */
    private int normalizeSize(Integer size) {
        // 默认 10 条。
        int value = size == null ? 10 : size;
        // 最小 1 条，最大 50 条。
        return Math.max(1, Math.min(value, 50));
    }

    /**
     * 构造游标分页响应。
     */
    private <T> CursorPageVO<T> page(List<T> rows, int size, Function<T, Long> idGetter) {
        // 多查一条用于判断是否还有更多。
        boolean hasMore = rows.size() > size;
        // 返回给前端的记录最多 size 条。
        List<T> records = hasMore ? new ArrayList<>(rows.subList(0, size)) : rows;
        // 下一页游标取当前页最后一条 ID。
        Long nextLastId = records.isEmpty() ? null : idGetter.apply(records.get(records.size() - 1));
        // 返回分页对象。
        return new CursorPageVO<>(hasMore, nextLastId, records);
    }

    /**
     * 判断亲情号用户 ID。
     */
    private Long familyUserId(ServiceOrder order) {
        // 只有亲情号创建的订单才返回 familyUserId。
        return RoleConstants.FAMILY.equals(order.getCreatorRole()) ? order.getCreatorUserId() : null;
    }

    /**
     * 组装志愿者摘要。
     */
    private VolunteerBriefVO volunteerBrief(ServiceOrder order) {
        // 没有分配志愿者时返回空。
        if (order.getAssignedVolunteerUserId() == null) {
            // 未接单订单没有志愿者。
            return null;
        }
        // 第一版详情中志愿者名称先用占位，避免跨服务详情接口阻塞订单详情。
        return new VolunteerBriefVO(order.getAssignedVolunteerUserId(), "志愿者" + order.getAssignedVolunteerUserId(), null);
    }
}
