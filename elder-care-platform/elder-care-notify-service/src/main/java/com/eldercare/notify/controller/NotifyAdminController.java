package com.eldercare.notify.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.response.Result;
import com.eldercare.notify.entity.NotifyRecord;
import com.eldercare.notify.mapper.NotifyRecordMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台通知记录查询接口。
 */
@RestController
@RequestMapping("/admin/notify-records")
@RequireRole(RoleConstants.ADMIN)
public class NotifyAdminController {
    /** 通知记录 Mapper。 */
    private final NotifyRecordMapper notifyRecordMapper;

    public NotifyAdminController(NotifyRecordMapper notifyRecordMapper) {
        // 保存通知记录 Mapper。
        this.notifyRecordMapper = notifyRecordMapper;
    }

    /**
     * 查询当前社区最近通知记录。
     *
     * @return 通知记录列表。
     */
    @GetMapping
    public Result<List<NotifyRecord>> list() {
        // 加载管理员上下文。
        UserContext.loadFromCurrentRequest();
        // 读取社区 ID。
        Long communityId = UserContext.requireCommunityId();
        // 查询最近 100 条通知记录。
        return Result.success(notifyRecordMapper.selectList(new LambdaQueryWrapper<NotifyRecord>().eq(NotifyRecord::getCommunityId, communityId).eq(NotifyRecord::getDeleted, 0).orderByDesc(NotifyRecord::getId).last("LIMIT 100")));
    }
}
