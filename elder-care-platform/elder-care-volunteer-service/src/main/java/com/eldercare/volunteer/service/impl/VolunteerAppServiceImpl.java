package com.eldercare.volunteer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.eldercare.api.dto.*;
import com.eldercare.api.vo.CursorPageVO;
import com.eldercare.api.vo.VolunteerCheckinRecordVO;
import com.eldercare.api.vo.VolunteerCheckinTodayVO;
import com.eldercare.api.vo.VolunteerBriefVO;
import com.eldercare.api.vo.VolunteerWorkbenchVO;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.context.UserInfoDTO;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.common.log.annotation.OperationLog;
import com.eldercare.common.log.enums.OperationModuleEnum;
import com.eldercare.common.log.enums.OperationTypeEnum;
import com.eldercare.volunteer.entity.VolunteerAvailableTime;
import com.eldercare.volunteer.entity.VolunteerCheckinRecord;
import com.eldercare.volunteer.entity.VolunteerProfile;
import com.eldercare.volunteer.entity.VolunteerTimeLock;
import com.eldercare.volunteer.mapper.VolunteerAvailableTimeMapper;
import com.eldercare.volunteer.mapper.VolunteerCheckinRecordMapper;
import com.eldercare.volunteer.mapper.VolunteerProfileMapper;
import com.eldercare.volunteer.mapper.VolunteerTimeLockMapper;
import com.eldercare.volunteer.service.VolunteerAppService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 志愿者业务实现。
 */
@Service
public class VolunteerAppServiceImpl implements VolunteerAppService {
    /**
     * 志愿者档案正常状态，表中 2 表示审核通过/可服务。
     */
    private static final int NORMAL = 2;

    /**
     * 可服务时间可用状态。
     */
    private static final int AVAILABLE = 1;

    /**
     * 时间锁已锁定状态。
     */
    private static final int LOCKED = 1;

    /**
     * 时间锁已释放状态。
     */
    private static final int RELEASED = 2;

    /**
     * 志愿者档案 Mapper。
     */
    private final VolunteerProfileMapper profileMapper;

    /**
     * 志愿者可服务时间 Mapper。
     */
    private final VolunteerAvailableTimeMapper availableMapper;

    /**
     * 志愿者时间锁 Mapper。
     */
    private final VolunteerTimeLockMapper lockMapper;

    /**
     * 志愿者签到记录 Mapper。
     */
    private final VolunteerCheckinRecordMapper checkinMapper;

    /**
     * Redisson 客户端，用于分布式锁。
     */
    private final RedissonClient redissonClient;

    public VolunteerAppServiceImpl(VolunteerProfileMapper profileMapper, VolunteerAvailableTimeMapper availableMapper, VolunteerTimeLockMapper lockMapper, VolunteerCheckinRecordMapper checkinMapper, RedissonClient redissonClient) {
        // 保存志愿者档案 Mapper。
        this.profileMapper = profileMapper;
        // 保存可服务时间 Mapper。
        this.availableMapper = availableMapper;
        // 保存时间锁 Mapper。
        this.lockMapper = lockMapper;
        // 保存签到 Mapper。
        this.checkinMapper = checkinMapper;
        // 保存 Redisson 客户端。
        this.redissonClient = redissonClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = OperationModuleEnum.VOLUNTEER, operationType = OperationTypeEnum.UPDATE,
            description = "完善或更新志愿者资料")
    public void saveProfile(VolunteerProfileDTO dto) {
        // 从网关透传的请求头中读取当前登录用户和社区。
        UserInfoDTO user = currentUser();
        // 一个志愿者只能属于一个社区，所以按 community_id + user_id 查询档案。
        VolunteerProfile profile = profileMapper.selectOne(new QueryWrapper<VolunteerProfile>().eq("community_id", user.communityId()).eq("user_id", user.userId()).eq("deleted", 0).last("limit 1"));
        // 没有档案时创建新档案。
        if (profile == null) {
            // 通过实体工厂创建档案，避免业务层散落初始化细节。
            profile = VolunteerProfile.create(user.communityId(), user.userId());
        }
        // 通过实体行为刷新档案字段，让资料状态变更规则集中在实体内部。
        profile.updateProfile(dto.volunteerName(), dto.volunteerPhone(), dto.skillTags(), dto.serviceRadiusMeter(), NORMAL);
        // 新档案执行插入。
        if (profile.getId() == null) {
            // 插入志愿者档案。
            profileMapper.insert(profile);
        } else {
            // 已有档案执行更新。
            profileMapper.updateById(profile);
        }
    }

    @Override
    @OperationLog(module = OperationModuleEnum.VOLUNTEER, operationType = OperationTypeEnum.CREATE,
            description = "新增志愿者可服务时间", bizId = "#dto.serviceItemId")
    public void addAvailableTime(VolunteerAvailableTimeDTO dto) {
        // 读取当前志愿者上下文。
        UserInfoDTO user = currentUser();
        // 通过实体工厂创建可服务时间，避免 Service 直接拼装表字段。
        VolunteerAvailableTime time = VolunteerAvailableTime.create(user.communityId(), user.userId(), dto.serviceItemId(), dto.startTime(), dto.endTime(), AVAILABLE);
        // 插入可服务时间。
        availableMapper.insert(time);
    }

    @Override
    public List<VolunteerBriefVO> listAvailable(AvailableVolunteerQueryDTO dto) {
        // 外部接口通过网关访问时，从请求头读取 community_id。
        Long communityId = UserContext.loadFromCurrentRequest() == null ? null : UserContext.getCommunityId();
        // 内部 Feign 调用旧接口暂时没有 communityId 入参时，第一版兜底演示社区 1。
        if (communityId == null) {
            // 使用演示社区 ID。
            communityId = 1L;
        }
        // lambda 中使用的局部变量必须是实际 final。
        Long queryCommunityId = communityId;
        // 查询覆盖目标时间段的可服务时间。
        QueryWrapper<VolunteerAvailableTime> qw = new QueryWrapper<VolunteerAvailableTime>()
                // 限定本社区。
                .eq("community_id", queryCommunityId)
                // 只看可用状态。
                .eq("available_status", AVAILABLE)
                // 排除逻辑删除。
                .eq("deleted", 0)
                // 志愿者可服务开始时间必须早于等于订单开始时间。
                .le("start_time", dto.startTime())
                // 志愿者可服务结束时间必须晚于等于订单结束时间。
                .ge("end_time", dto.endTime())
                // service_item_id 为空表示不限项目，否则必须匹配指定服务项目。
                .and(w -> w.isNull("service_item_id").or().eq("service_item_id", dto.serviceItemId()));
        // 提取候选志愿者 ID 并去重。
        List<Long> ids = availableMapper.selectList(qw).stream().map(VolunteerAvailableTime::getVolunteerUserId).distinct().toList();
        // 再经过时间锁冲突校验，过滤掉已被占用的志愿者。
        return ids.stream().filter(id -> checkAvailable(new VolunteerCheckAvailableDTO(queryCommunityId, id, dto.serviceItemId(), dto.startTime(), dto.endTime())))
                // 第一版返回简要信息，姓名先用占位格式。
                .map(id -> new VolunteerBriefVO(id, "志愿者" + id, null)).toList();
    }

    @Override
    public boolean checkAvailable(VolunteerCheckAvailableDTO dto) {
        // 查询志愿者是否有覆盖该订单时间段的可服务时间。
        Long count = availableMapper.selectCount(new QueryWrapper<VolunteerAvailableTime>()
                // 必须属于同一社区。
                .eq("community_id", dto.communityId())
                // 必须是目标志愿者。
                .eq("volunteer_user_id", dto.volunteerUserId())
                // 必须是可用时间段。
                .eq("available_status", AVAILABLE)
                // 排除逻辑删除。
                .eq("deleted", 0)
                // 可服务开始时间覆盖订单开始时间。
                .le("start_time", dto.startTime())
                // 可服务结束时间覆盖订单结束时间。
                .ge("end_time", dto.endTime())
                // 服务项目为空表示通用，否则必须匹配。
                .and(w -> w.isNull("service_item_id").or().eq("service_item_id", dto.serviceItemId())));
        // 查询志愿者是否存在已锁定且与目标时间段重叠的记录。
        Long locked = lockMapper.selectCount(new QueryWrapper<VolunteerTimeLock>()
                // 同社区才比较。
                .eq("community_id", dto.communityId())
                // 同志愿者才比较。
                .eq("volunteer_user_id", dto.volunteerUserId())
                // 只看活跃锁。
                .eq("lock_status", LOCKED)
                // 排除逻辑删除。
                .eq("deleted", 0)
                // 区间重叠判断：已有开始 < 新结束。
                .lt("start_time", dto.endTime())
                // 区间重叠判断：已有结束 > 新开始。
                .gt("end_time", dto.startTime()));
        // 有可服务时间，且没有时间锁冲突，才算可用。
        return count != null && count > 0 && (locked == null || locked == 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockTime(VolunteerLockTimeDTO dto) {
        // Redisson 锁 key 按社区、志愿者、时间槽拆分，降低锁粒度。
        RLock lock = redissonClient.getLock("lock:volunteer:time:" + dto.communityId() + ":" + dto.volunteerUserId() + ":" + slotKey(dto));
        try {
            // 最多等待 5 秒，锁自动过期 10 秒，避免服务异常导致死锁。
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                // 没拿到锁说明并发竞争激烈。
                return false;
            }
            // 分布式锁内再次检查可用性，避免锁等待期间状态变化。
            if (!checkAvailable(new VolunteerCheckAvailableDTO(dto.communityId(), dto.volunteerUserId(), dto.serviceItemId(), dto.startTime(), dto.endTime()))) {
                // 志愿者时间不可用。
                return false;
            }
            // 通过实体工厂创建时间锁，锁状态和唯一时间槽的初始化集中在实体内。
            VolunteerTimeLock row = VolunteerTimeLock.locked(dto.communityId(), dto.volunteerUserId(), dto.orderId(), dto.startTime(), dto.endTime(), slotKey(dto), LOCKED);
            // 插入时间锁，若唯一索引冲突会抛 DuplicateKeyException。
            lockMapper.insert(row);
            // 插入成功表示锁定成功。
            return true;
        } catch (DuplicateKeyException e) {
            // 数据库唯一索引兜底防止并发重复锁定。
            return false;
        } catch (InterruptedException e) {
            // 恢复线程中断标记。
            Thread.currentThread().interrupt();
            // 转换为业务异常交给全局异常处理。
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "获取志愿者时间锁失败");
        } finally {
            // 只有当前线程持有锁时才能释放。
            if (lock.isHeldByCurrentThread()) {
                // 释放 Redisson 分布式锁。
                lock.unlock();
            }
        }
    }

    @Override
    public boolean releaseTime(VolunteerLockTimeDTO dto) {
        // 按订单和志愿者释放活跃时间锁。
        return lockMapper.update(null, new UpdateWrapper<VolunteerTimeLock>()
                // 将锁状态改为已释放。
                .set("lock_status", RELEASED)
                // 限定社区。
                .eq("community_id", dto.communityId())
                // 限定订单。
                .eq("order_id", dto.orderId())
                // 限定志愿者。
                .eq("volunteer_user_id", dto.volunteerUserId())
                // 只释放已锁定记录。
                .eq("lock_status", LOCKED)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = OperationModuleEnum.CHECKIN, operationType = OperationTypeEnum.CHECKIN,
            description = "志愿者今日签到", recordResult = false)
    public void checkIn(VolunteerCheckinDTO dto) {
        // 读取当前志愿者上下文。
        UserInfoDTO user = currentUser();
        try {
            // 插入今日签到记录，唯一索引保证同一天只能签到一次。
            checkinMapper.insert(VolunteerCheckinRecord.today(user.communityId(), user.userId(), dto.longitude(), dto.latitude(), dto.address()));
        } catch (DuplicateKeyException exception) {
            // 唯一索引冲突说明今天已经签到。
            throw new BizException(ErrorCode.DATA_EXISTS, "今天已经签到");
        }
    }

    @Override
    public VolunteerCheckinTodayVO todayCheckin() {
        // 读取当前志愿者上下文。
        UserInfoDTO user = currentUser();
        // 查询今天签到记录。
        VolunteerCheckinRecord record = selectTodayCheckin(user);
        // 未签到时返回 checkedIn=false。
        if (record == null) {
            // 返回未签到状态。
            return new VolunteerCheckinTodayVO(false, null, null);
        }
        // 返回签到时间和地址。
        return new VolunteerCheckinTodayVO(true, record.getCheckinTime(), record.getAddress());
    }

    @Override
    public CursorPageVO<VolunteerCheckinRecordVO> checkinPage(Long lastId, Integer size, LocalDate startDate, LocalDate endDate) {
        // 读取当前志愿者上下文。
        UserInfoDTO user = currentUser();
        // 构造查询，限定当前志愿者自己。
        QueryWrapper<VolunteerCheckinRecord> qw = new QueryWrapper<VolunteerCheckinRecord>()
                .eq("community_id", user.communityId())
                .eq("volunteer_user_id", user.userId());
        // 开始日期可选。
        if (startDate != null) {
            // 限定签到日期大于等于开始日期。
            qw.ge("checkin_date", startDate);
        }
        // 结束日期可选。
        if (endDate != null) {
            // 限定签到日期小于等于结束日期。
            qw.le("checkin_date", endDate);
        }
        // 应用游标分页。
        applyCursor(qw, lastId, normalizeSize(size));
        // 查询并转换记录。
        List<VolunteerCheckinRecordVO> records = checkinMapper.selectList(qw).stream()
                .map(r -> new VolunteerCheckinRecordVO(r.getId(), r.getCheckinDate(), r.getCheckinTime(), r.getLongitude(), r.getLatitude(), r.getAddress(), r.getStatus()))
                .toList();
        // 返回分页对象。
        return page(records, normalizeSize(size), VolunteerCheckinRecordVO::id);
    }

    @Override
    public VolunteerWorkbenchVO workbench() {
        // 读取当前志愿者上下文。
        UserInfoDTO user = currentUser();
        // 查询志愿者资料。
        VolunteerProfile profile = profileMapper.selectOne(new QueryWrapper<VolunteerProfile>()
                .eq("community_id", user.communityId())
                .eq("user_id", user.userId())
                .eq("deleted", 0)
                .last("limit 1"));
        // 本地服务先返回签到、评分占位和接单状态，订单统计后续通过 Feign 精细化。
        boolean checkedIn = selectTodayCheckin(user) != null;
        // 评分第一版用 0 占位，评价聚合由 order-service 提供。
        Integer score = 0;
        // 有正常资料且已签到时表示可以接单。
        String acceptStatus = profile != null && checkedIn ? "AVAILABLE" : "UNAVAILABLE";
        // 返回工作台数据。
        return new VolunteerWorkbenchVO(checkedIn, 0, 0, 0, 0, score, acceptStatus);
    }

    private UserInfoDTO currentUser() {
        // 从当前 HTTP 请求头读取用户上下文。
        UserContext.loadFromCurrentRequest();
        // 校验 community_id 必须存在。
        UserContext.requireCommunityId();
        // 返回当前用户上下文。
        return UserContext.get();
    }

    private String slotKey(VolunteerLockTimeDTO dto) {
        // 时间槽精确到分钟，和数据库唯一索引中的 time_slot_key 对齐。
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        // 拼接开始结束时间形成稳定 key。
        return dto.startTime().format(f) + "_" + dto.endTime().format(f);
    }

    /**
     * 查询今日签到记录。
     */
    private VolunteerCheckinRecord selectTodayCheckin(UserInfoDTO user) {
        // 按志愿者和当天日期查询。
        return checkinMapper.selectOne(new QueryWrapper<VolunteerCheckinRecord>()
                .eq("community_id", user.communityId())
                .eq("volunteer_user_id", user.userId())
                .eq("checkin_date", LocalDate.now())
                .last("limit 1"));
    }

    /**
     * 应用游标分页。
     */
    private void applyCursor(QueryWrapper<?> qw, Long lastId, int size) {
        // 传入 lastId 时查更早记录。
        if (lastId != null) {
            // 添加 id < lastId 条件。
            qw.lt("id", lastId);
        }
        // 按 ID 倒序并多查一条判断 hasMore。
        qw.orderByDesc("id").last("limit " + (size + 1));
    }

    /**
     * 规整分页大小。
     */
    private int normalizeSize(Integer size) {
        // 默认 10，最大 50。
        return Math.max(1, Math.min(size == null ? 10 : size, 50));
    }

    /**
     * 构造分页响应。
     */
    private <T> CursorPageVO<T> page(List<T> rows, int size, Function<T, Long> idGetter) {
        // 多查一条时，大于 size 表示还有更多。
        boolean hasMore = rows.size() > size;
        // 截取实际返回记录。
        List<T> records = hasMore ? new ArrayList<>(rows.subList(0, size)) : rows;
        // 下一页游标取最后一条记录 ID。
        Long nextLastId = records.isEmpty() ? null : idGetter.apply(records.get(records.size() - 1));
        // 返回游标分页结果。
        return new CursorPageVO<>(hasMore, nextLastId, records);
    }
}
