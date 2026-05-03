package com.eldercare.common.constant;

/**
 * 用户角色常量，避免业务代码中散落硬编码字符串。
 */
public final class RoleConstants {

    /**
     * 老人角色。
     */
    public static final String ELDER = "ELDER";

    /**
     * 亲情号角色。
     */
    public static final String FAMILY = "FAMILY";

    /**
     * 志愿者或服务人员角色。
     */
    public static final String VOLUNTEER = "VOLUNTEER";

    /**
     * 私有构造方法，防止常量类被实例化。
     */
    private RoleConstants() {
        // 常量类不需要创建对象。
    }
}
