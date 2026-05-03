package com.eldercare.common.constant;

/**
 * 微服务注册到 Nacos 时使用的服务名常量。
 */
public final class ServiceNames {

    /**
     * 用户服务名。
     */
    public static final String USER_SERVICE = "elder-care-user-service";

    /**
     * 社区服务名。
     */
    public static final String COMMUNITY_SERVICE = "elder-care-community-service";

    /**
     * 志愿者服务名。
     */
    public static final String VOLUNTEER_SERVICE = "elder-care-volunteer-service";

    /**
     * 订单服务名。
     */
    public static final String ORDER_SERVICE = "elder-care-order-service";

    /**
     * 通知服务名。
     */
    public static final String NOTIFY_SERVICE = "elder-care-notify-service";

    /**
     * 私有构造方法，防止工具常量类被实例化。
     */
    private ServiceNames() {
        // 常量类不需要创建对象。
    }
}
