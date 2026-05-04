package com.eldercare.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.eldercare.api.vo.CursorPageVO;
import com.eldercare.api.vo.NoticeVO;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.context.UserInfoDTO;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.notify.entity.NotifyRecord;
import com.eldercare.notify.mapper.NotifyRecordMapper;
import com.eldercare.notify.service.NoticeAppService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 小程序消息中心业务实现。
 */
@Service
public class NoticeAppServiceImpl implements NoticeAppService {
    /** 通知记录 Mapper。 */
    private final NotifyRecordMapper notifyRecordMapper;

    public NoticeAppServiceImpl(NotifyRecordMapper notifyRecordMapper) {
        // 保存通知记录 Mapper。
        this.notifyRecordMapper = notifyRecordMapper;
    }

    @Override
    public CursorPageVO<NoticeVO> myPage(Long lastId, Integer size, Integer readStatus) {
        // 读取当前登录用户。
        UserInfoDTO user = currentUser();
        // 构造消息查询，限定用户和社区。
        QueryWrapper<NotifyRecord> qw = new QueryWrapper<NotifyRecord>()
                .eq("community_id", user.communityId())
                .eq("receiver_user_id", user.userId())
                .eq("deleted", 0);
        // 已读状态可选过滤。
        if (readStatus != null) {
            // 按 read_status 过滤。
            qw.eq("read_status", readStatus);
        }
        // 应用游标分页。
        applyCursor(qw, lastId, normalizeSize(size));
        // 查询并转换消息记录。
        List<NoticeVO> records = notifyRecordMapper.selectList(qw).stream()
                .map(r -> new NoticeVO(r.getId(), r.getNotifyTitle(), r.getNotifyContent(), r.getBusinessType(), r.getReadStatus(), r.getBusinessType(), r.getBusinessId(), r.getCreatedAt()))
                .toList();
        // 返回分页响应。
        return page(records, normalizeSize(size), NoticeVO::id);
    }

    @Override
    public long unreadCount() {
        // 读取当前登录用户。
        UserInfoDTO user = currentUser();
        // 查询未读数量。
        Long count = notifyRecordMapper.selectCount(new QueryWrapper<NotifyRecord>()
                .eq("community_id", user.communityId())
                .eq("receiver_user_id", user.userId())
                .eq("read_status", 0)
                .eq("deleted", 0));
        // 空值按 0 处理。
        return count == null ? 0L : count;
    }

    @Override
    public void read(Long noticeId) {
        // 读取当前登录用户。
        UserInfoDTO user = currentUser();
        // 只允许更新自己的消息。
        int updated = notifyRecordMapper.update(null, new UpdateWrapper<NotifyRecord>()
                .set("read_status", 1)
                .eq("id", noticeId)
                .eq("community_id", user.communityId())
                .eq("receiver_user_id", user.userId())
                .eq("deleted", 0));
        // 更新不到说明消息不存在或越权。
        if (updated != 1) {
            // 抛出资源不存在。
            throw new BizException(ErrorCode.NOT_FOUND, "消息不存在");
        }
    }

    @Override
    public void readAll() {
        // 读取当前登录用户。
        UserInfoDTO user = currentUser();
        // 当前用户所有未读消息改为已读。
        notifyRecordMapper.update(null, new UpdateWrapper<NotifyRecord>()
                .set("read_status", 1)
                .eq("community_id", user.communityId())
                .eq("receiver_user_id", user.userId())
                .eq("read_status", 0)
                .eq("deleted", 0));
    }

    /** 读取当前用户上下文。 */
    private UserInfoDTO currentUser() {
        // 从请求头加载用户信息。
        UserContext.loadFromCurrentRequest();
        // 校验登录用户。
        UserContext.requireLoginUser();
        // 校验社区 ID。
        UserContext.requireCommunityId();
        // 返回上下文。
        return UserContext.get();
    }

    /** 应用游标分页。 */
    private void applyCursor(QueryWrapper<?> qw, Long lastId, int size) {
        // lastId 存在时查询更早记录。
        if (lastId != null) {
            // 使用 id < lastId 避免深分页。
            qw.lt("id", lastId);
        }
        // 按 id 倒序并多查一条判断是否还有更多。
        qw.orderByDesc("id").last("limit " + (size + 1));
    }

    /** 规整分页大小。 */
    private int normalizeSize(Integer size) {
        // 默认 10，最大 50。
        return Math.max(1, Math.min(size == null ? 10 : size, 50));
    }

    /** 构造游标分页响应。 */
    private <T> CursorPageVO<T> page(List<T> rows, int size, Function<T, Long> idGetter) {
        // 多查一条说明有更多。
        boolean hasMore = rows.size() > size;
        // 截取真实返回数据。
        List<T> records = hasMore ? new ArrayList<>(rows.subList(0, size)) : rows;
        // 下一页游标。
        Long nextLastId = records.isEmpty() ? null : idGetter.apply(records.get(records.size() - 1));
        // 返回分页对象。
        return new CursorPageVO<>(hasMore, nextLastId, records);
    }
}
