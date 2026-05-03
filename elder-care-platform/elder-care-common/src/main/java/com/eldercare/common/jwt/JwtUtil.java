package com.eldercare.common.jwt;

import com.eldercare.common.context.LoginUser;
import com.eldercare.common.context.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具类，负责生成和解析登录令牌。
 */
@Component
public class JwtUtil {

    /**
     * JWT 配置属性。
     */
    private final JwtProperties jwtProperties;

    /**
     * 构造 JWT 工具类。
     *
     * @param jwtProperties JWT 配置属性。
     */
    public JwtUtil(JwtProperties jwtProperties) {
        // 保存配置对象，生成和解析 Token 时都会用到。
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成登录 Token。
     *
     * @param loginUser 登录用户信息。
     * @return JWT 字符串。
     */
    public String generateToken(LoginUser loginUser) {
        // 当前时间作为签发时间。
        Instant now = Instant.now();
        // 根据过期秒数计算 Token 过期时间。
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());
        // 使用 JJWT 构造并签名 Token。
        return Jwts.builder()
                .subject(String.valueOf(loginUser.userId()))
                .claim("communityId", loginUser.communityId())
                .claim("roles", loginUser.roles())
                .claim("role", loginUser.role() == null ? null : loginUser.role().name())
                .claim("phone", loginUser.phone())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey())
                .compact();
    }

    /**
     * 解析登录 Token。
     *
     * @param token JWT 字符串。
     * @return 登录用户信息。
     */
    public LoginUser parseToken(String token) {
        // 解析签名并读取 Claims，签名错误或过期会抛出异常。
        Claims claims = Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        // subject 存放用户 ID。
        Long userId = Long.valueOf(claims.getSubject());
        // communityId 存放当前登录社区 ID。
        Long communityId = parseLong(claims.get("communityId"));
        // roles 存放用户多角色列表。
        List<String> roles = parseRoles(claims.get("roles"), claims.get("role"));
        // role 存放兼容早期单角色的主角色。
        UserRole role = roles.isEmpty() ? null : UserRole.valueOf(roles.get(0));
        // phone 存放手机号。
        String phone = String.valueOf(claims.get("phone"));
        // 返回业务系统内部使用的登录用户对象。
        return new LoginUser(userId, communityId, roles, role, phone);
    }

    /**
     * 将 JWT Claim 转换为 Long。
     *
     * @param value Claim 原始值。
     * @return Long 值；为空时返回 null。
     */
    private Long parseLong(Object value) {
        // Claim 不存在时返回 null。
        if (value == null) {
            // 没有社区 ID。
            return null;
        }
        // 转成字符串后解析 Long。
        return Long.valueOf(String.valueOf(value));
    }

    /**
     * 解析多角色 Claim，并兼容旧的单角色 role Claim。
     *
     * @param rolesValue 多角色 Claim。
     * @param roleValue  单角色 Claim。
     * @return 角色字符串列表。
     */
    @SuppressWarnings("unchecked")
    private List<String> parseRoles(Object rolesValue, Object roleValue) {
        // roles 为 JSON 数组时，JJWT 会解析成 List。
        if (rolesValue instanceof List<?> values) {
            // 转成字符串列表，避免泛型擦除带来的类型风险。
            return values.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .filter(value -> !"null".equals(value))
                    .toList();
        }
        // 兼容 roles 为单字符串的情况。
        if (rolesValue != null) {
            // 使用逗号切分字符串角色。
            return java.util.Arrays.stream(String.valueOf(rolesValue).split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .filter(value -> !"null".equals(value))
                    .toList();
        }
        // 兼容旧 Token 的 role 字段。
        if (roleValue != null && !"null".equals(String.valueOf(roleValue))) {
            // 单角色转换成单元素列表。
            return List.of(String.valueOf(roleValue));
        }
        // 没有任何角色时返回空列表。
        return new ArrayList<>();
    }

    /**
     * 根据配置中的 secret 构造 HS256 签名密钥。
     *
     * @return HMAC 签名密钥。
     */
    private SecretKey secretKey() {
        // JJWT 要求 HMAC 密钥长度足够，否则会抛出 WeakKeyException。
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
