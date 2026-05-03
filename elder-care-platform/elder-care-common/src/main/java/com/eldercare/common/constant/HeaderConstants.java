package com.eldercare.common.constant;

/**
 * 网关和后端服务之间透传用户信息时使用的请求头常量。
 */
public final class HeaderConstants {

    /**
     * 用户 ID 请求头，由网关解析 JWT 后写入。
     */
    public static final String USER_ID = "X-User-Id";

    /**
     * 社区 ID 请求头，由网关解析 JWT 或前置租户逻辑后写入。
     */
    public static final String COMMUNITY_ID = "X-Community-Id";

    /**
     * 单角色请求头，兼容早期只透传一个角色的实现。
     */
    public static final String USER_ROLE = "X-User-Role";

    /**
     * 多角色请求头，多个角色使用英文逗号分隔，例如 ELDER,FAMILY。
     */
    public static final String ROLES = "X-Roles";

    /**
     * 用户手机号请求头，由网关解析 JWT 后写入。
     */
    public static final String USER_PHONE = "X-User-Phone";

    /**
     * 私有构造方法，防止工具常量类被实例化。
     */
    private HeaderConstants() {
        // 常量类不需要创建对象。
    }
}
