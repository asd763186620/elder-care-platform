package com.eldercare.common.context;

import com.eldercare.common.constant.HeaderConstants;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * 用户上下文，用 ThreadLocal 保存当前请求的用户信息。
 */
public final class UserContext {

    /**
     * ThreadLocal 为每个请求线程保存独立的用户上下文。
     */
    private static final ThreadLocal<UserInfoDTO> USER_INFO_HOLDER = new ThreadLocal<>();

    /**
     * 私有构造方法，防止工具类被实例化。
     */
    private UserContext() {
        // 工具类不需要创建对象。
    }

    /**
     * 设置当前线程的用户上下文。
     *
     * @param userInfo 用户上下文。
     */
    public static void set(UserInfoDTO userInfo) {
        // 将当前请求的用户信息写入 ThreadLocal。
        USER_INFO_HOLDER.set(userInfo);
    }

    /**
     * 兼容旧代码：根据 LoginUser 设置当前线程的用户上下文。
     *
     * @param loginUser 登录用户信息。
     */
    public static void set(LoginUser loginUser) {
        // LoginUser 已包含多角色和社区 ID，直接转换为 UserInfoDTO。
        USER_INFO_HOLDER.set(new UserInfoDTO(loginUser.userId(), loginUser.communityId(), loginUser.roles()));
    }

    /**
     * 获取当前线程的用户上下文。
     *
     * @return 用户上下文；没有用户信息时返回 null。
     */
    public static UserInfoDTO get() {
        // 从 ThreadLocal 中读取当前请求的用户信息。
        return USER_INFO_HOLDER.get();
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 当前用户 ID。
     */
    public static Long getUserId() {
        // 读取当前用户上下文。
        UserInfoDTO userInfo = get();
        // 上下文不存在时返回 null。
        return userInfo == null ? null : userInfo.userId();
    }

    /**
     * 获取当前社区 ID。
     *
     * @return 当前社区 ID。
     */
    public static Long getCommunityId() {
        // 读取当前用户上下文。
        UserInfoDTO userInfo = get();
        // 上下文不存在时返回 null。
        return userInfo == null ? null : userInfo.communityId();
    }

    /**
     * 获取当前用户角色列表。
     *
     * @return 当前用户角色列表。
     */
    public static List<String> getRoles() {
        // 读取当前用户上下文。
        UserInfoDTO userInfo = get();
        // 上下文不存在时返回空列表。
        return userInfo == null || userInfo.roles() == null ? List.of() : userInfo.roles();
    }

    /**
     * 判断当前用户是否拥有指定角色。
     *
     * @param roleCode 角色编码。
     * @return true 表示拥有该角色。
     */
    public static boolean hasRole(String roleCode) {
        // 复用角色列表判断逻辑。
        return getRoles().contains(roleCode);
    }

    /**
     * 获取当前用户上下文，不存在时直接抛出未登录异常。
     *
     * @return 当前用户上下文。
     */
    public static UserInfoDTO requireLoginUser() {
        // 读取当前用户上下文。
        UserInfoDTO userInfo = get();
        // 没有用户上下文说明请求未登录或网关未透传。
        if (userInfo == null || userInfo.userId() == null) {
            // 抛出统一未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        // 返回已校验的用户上下文。
        return userInfo;
    }

    /**
     * 获取当前社区 ID，不存在时直接抛出异常。
     *
     * @return 当前社区 ID。
     */
    public static Long requireCommunityId() {
        // 先确保用户已登录。
        UserInfoDTO userInfo = requireLoginUser();
        // 社区 ID 缺失时说明网关或调用方没有传社区上下文。
        if (userInfo.communityId() == null) {
            // 抛出缺少社区上下文异常。
            throw new BizException(ErrorCode.COMMUNITY_REQUIRED);
        }
        // 返回社区 ID。
        return userInfo.communityId();
    }

    /**
     * 从当前 HTTP 请求头读取用户上下文并写入 ThreadLocal。
     *
     * @return 解析后的用户上下文；非 Servlet 请求时返回 null。
     */
    public static UserInfoDTO loadFromCurrentRequest() {
        // 从 Spring 请求上下文中读取当前请求属性。
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 非 Web 请求或 WebFlux 请求没有 ServletRequestAttributes。
        if (attributes == null) {
            // 无法从请求头读取用户上下文。
            return null;
        }
        // 从 Servlet 请求对象读取请求头。
        return loadFromRequest(attributes.getRequest());
    }

    /**
     * 从指定 HTTP 请求头读取用户上下文并写入 ThreadLocal。
     *
     * @param request HTTP 请求。
     * @return 解析后的用户上下文；请求头不完整时返回 null。
     */
    public static UserInfoDTO loadFromRequest(HttpServletRequest request) {
        // 读取用户 ID 请求头。
        String userIdHeader = request.getHeader(HeaderConstants.USER_ID);
        // 读取社区 ID 请求头。
        String communityIdHeader = request.getHeader(HeaderConstants.COMMUNITY_ID);
        // 优先读取多角色请求头。
        String rolesHeader = request.getHeader(HeaderConstants.ROLES);
        // 兼容旧的单角色请求头。
        String userRoleHeader = request.getHeader(HeaderConstants.USER_ROLE);
        // 用户 ID 为空时说明当前请求没有登录用户上下文。
        if (!StringUtils.hasText(userIdHeader)) {
            // 不写入 ThreadLocal。
            return null;
        }
        // 解析用户 ID。
        Long userId = Long.valueOf(userIdHeader);
        // 社区 ID 可以为空，部分内部接口或早期登录接口可能暂时没有社区。
        Long communityId = StringUtils.hasText(communityIdHeader) ? Long.valueOf(communityIdHeader) : null;
        // 解析角色列表。
        List<String> roles = parseRoles(StringUtils.hasText(rolesHeader) ? rolesHeader : userRoleHeader);
        // 构造用户上下文对象。
        UserInfoDTO userInfo = new UserInfoDTO(userId, communityId, roles);
        // 写入 ThreadLocal，方便当前请求后续业务代码读取。
        set(userInfo);
        // 返回解析结果。
        return userInfo;
    }

    /**
     * 清理当前线程的登录用户，避免线程复用时串数据。
     */
    public static void clear() {
        // 请求结束后必须清理 ThreadLocal。
        USER_INFO_HOLDER.remove();
    }

    /**
     * 解析英文逗号分隔的角色请求头。
     *
     * @param rolesHeader 角色请求头。
     * @return 角色列表。
     */
    private static List<String> parseRoles(String rolesHeader) {
        // 角色请求头为空时返回空列表。
        if (!StringUtils.hasText(rolesHeader)) {
            // 没有角色。
            return List.of();
        }
        // 按英文逗号切分角色，并去除空白项。
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
