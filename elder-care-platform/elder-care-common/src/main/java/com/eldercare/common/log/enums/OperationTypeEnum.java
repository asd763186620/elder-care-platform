package com.eldercare.common.log.enums;

/**
 * 操作类型枚举。
 */
public enum OperationTypeEnum {
    /** 新增。 */
    CREATE,
    /** 修改。 */
    UPDATE,
    /** 删除。 */
    DELETE,
    /** 状态变更。 */
    STATUS_CHANGE,
    /** 抢单。 */
    GRAB_ORDER,
    /** 取消订单。 */
    CANCEL_ORDER,
    /** 签到。 */
    CHECKIN,
    /** 完成订单。 */
    COMPLETE_ORDER,
    /** 绑定。 */
    BIND,
    /** 登录。 */
    LOGIN,
    /** 退出登录。 */
    LOGOUT
}
