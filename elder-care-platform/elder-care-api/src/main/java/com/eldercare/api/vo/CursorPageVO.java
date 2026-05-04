package com.eldercare.api.vo;

import java.util.List;

/**
 * 游标分页响应。
 *
 * @param hasMore    是否还有下一页。
 * @param nextLastId 下一页请求要传的 lastId。
 * @param records    当前页记录。
 * @param <T>        记录类型。
 */
public record CursorPageVO<T>(
        // true 表示前端可以继续请求下一页。
        boolean hasMore,
        // 下一页游标；没有更多数据时可以为空。
        Long nextLastId,
        // 当前页数据。
        List<T> records
) {
}
