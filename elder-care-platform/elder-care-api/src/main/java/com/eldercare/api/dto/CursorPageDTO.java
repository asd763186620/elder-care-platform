package com.eldercare.api.dto;

/**
 * 游标分页请求。
 *
 * @param lastId 上一页最后一条记录 ID，第一页可以不传。
 * @param size   每页数量，服务端会限制最大值，避免一次拉取过多数据。
 */
public record CursorPageDTO(
        // 游标 ID；查询条件会使用 id < lastId，避免 MySQL 深分页。
        Long lastId,
        // 每页条数；为空时业务层使用默认值。
        Integer size
) {
}
