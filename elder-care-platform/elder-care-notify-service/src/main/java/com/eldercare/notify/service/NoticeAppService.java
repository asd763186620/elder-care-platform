package com.eldercare.notify.service;

import com.eldercare.api.vo.CursorPageVO;
import com.eldercare.api.vo.NoticeVO;

/**
 * 小程序消息中心业务接口。
 */
public interface NoticeAppService {
    /** 查询我的消息分页。 */
    CursorPageVO<NoticeVO> myPage(Long lastId, Integer size, Integer readStatus);

    /** 查询未读消息数。 */
    long unreadCount();

    /** 标记单条已读。 */
    void read(Long noticeId);

    /** 标记当前用户全部已读。 */
    void readAll();
}
