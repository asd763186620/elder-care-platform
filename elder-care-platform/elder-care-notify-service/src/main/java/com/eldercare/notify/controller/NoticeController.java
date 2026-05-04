package com.eldercare.notify.controller;

import com.eldercare.api.vo.CursorPageVO;
import com.eldercare.api.vo.NoticeVO;
import com.eldercare.common.response.Result;
import com.eldercare.notify.service.NoticeAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序消息中心接口。
 */
@RestController
@RequestMapping("/notices")
@Tag(name = "消息中心接口", description = "小程序消息分页、未读数和已读操作")
public class NoticeController {
    /** 消息业务服务。 */
    private final NoticeAppService noticeAppService;

    public NoticeController(NoticeAppService noticeAppService) {
        // 保存消息业务服务。
        this.noticeAppService = noticeAppService;
    }

    /** 我的消息分页。 */
    @GetMapping("/my/page")
    @Operation(summary = "我的消息分页", description = "按当前用户和社区隔离查询消息")
    public Result<CursorPageVO<NoticeVO>> myPage(@RequestParam(required = false) Long lastId,
                                                 @RequestParam(required = false) Integer size,
                                                 @RequestParam(required = false) Integer readStatus) {
        return Result.success(noticeAppService.myPage(lastId, size, readStatus));
    }

    /** 未读消息数。 */
    @GetMapping("/unread-count")
    @Operation(summary = "未读消息数", description = "查询当前用户未读消息数量")
    public Result<Long> unreadCount() {
        return Result.success(noticeAppService.unreadCount());
    }

    /** 标记单条已读。 */
    @PostMapping("/{noticeId}/read")
    @Operation(summary = "标记单条已读", description = "只能操作当前用户自己的消息")
    public Result<Void> read(@PathVariable Long noticeId) {
        noticeAppService.read(noticeId);
        return Result.success();
    }

    /** 全部已读。 */
    @PostMapping("/read-all")
    @Operation(summary = "全部已读", description = "当前用户所有未读消息标记为已读")
    public Result<Void> readAll() {
        noticeAppService.readAll();
        return Result.success();
    }
}
