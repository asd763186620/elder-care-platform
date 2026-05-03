package com.eldercare.common.page;

import java.util.List;

/**
 * 通用分页响应对象。
 *
 * @param records  当前页数据。
 * @param total    总记录数。
 * @param pageNo   当前页码。
 * @param pageSize 每页数量。
 * @param pages    总页数。
 * @param <T>      数据类型。
 */
public record PageResult<T>(List<T> records, Long total, Integer pageNo, Integer pageSize, Long pages) {

    /**
     * 创建分页响应对象。
     *
     * @param records  当前页数据。
     * @param total    总记录数。
     * @param pageNo   当前页码。
     * @param pageSize 每页数量。
     * @param <T>      数据类型。
     * @return 分页响应对象。
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Integer pageNo, Integer pageSize) {
        // 空 total 按 0 处理。
        long safeTotal = total == null ? 0L : total;
        // 空 pageSize 或非法 pageSize 按 10 处理。
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        // 根据总数和每页数量计算总页数。
        long pages = safeTotal == 0 ? 0 : (safeTotal + safePageSize - 1) / safePageSize;
        // 空记录列表按空集合处理，避免前端空指针。
        List<T> safeRecords = records == null ? List.of() : records;
        // 返回标准分页响应对象。
        return new PageResult<>(safeRecords, safeTotal, pageNo, safePageSize, pages);
    }

    /**
     * 创建空分页响应。
     *
     * @param pageNo   当前页码。
     * @param pageSize 每页数量。
     * @param <T>      数据类型。
     * @return 空分页响应对象。
     */
    public static <T> PageResult<T> empty(Integer pageNo, Integer pageSize) {
        // 空分页总数为 0，记录为空列表。
        return of(List.of(), 0L, pageNo, pageSize);
    }
}
