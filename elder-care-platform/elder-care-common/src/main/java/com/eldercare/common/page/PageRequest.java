package com.eldercare.common.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 通用分页请求对象。
 *
 * @param pageNo   当前页码，从 1 开始。
 * @param pageSize 每页数量。
 */
public record PageRequest(
        // 页码最小为 1。
        @Min(value = 1, message = "不能小于1") Integer pageNo,
        // 每页数量最小为 1，最大为 100，避免一次查询过多数据。
        @Min(value = 1, message = "不能小于1") @Max(value = 100, message = "不能大于100") Integer pageSize
) {

    /**
     * 默认页码。
     */
    private static final int DEFAULT_PAGE_NO = 1;

    /**
     * 默认每页数量。
     */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 获取安全页码。
     *
     * @return 不为空且大于 0 的页码。
     */
    public int safePageNo() {
        // pageNo 为空或非法时使用默认值。
        return pageNo == null || pageNo < 1 ? DEFAULT_PAGE_NO : pageNo;
    }

    /**
     * 获取安全每页数量。
     *
     * @return 不为空且在合理范围内的每页数量。
     */
    public int safePageSize() {
        // pageSize 为空或非法时使用默认值。
        if (pageSize == null || pageSize < 1) {
            // 返回默认每页数量。
            return DEFAULT_PAGE_SIZE;
        }
        // 每页数量最多允许 100。
        return Math.min(pageSize, 100);
    }

    /**
     * 计算 SQL offset。
     *
     * @return SQL 分页偏移量。
     */
    public long offset() {
        // MySQL LIMIT offset,pageSize 的 offset 从 0 开始。
        return (long) (safePageNo() - 1) * safePageSize();
    }
}
