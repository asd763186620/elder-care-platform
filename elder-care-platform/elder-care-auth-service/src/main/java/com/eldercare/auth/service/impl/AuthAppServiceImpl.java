package com.eldercare.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.api.dto.MiniAppLoginDTO;
import com.eldercare.api.dto.PhoneBindDTO;
import com.eldercare.api.dto.SwitchRoleDTO;
import com.eldercare.api.dto.WechatPhoneBindDTO;
import com.eldercare.api.vo.AuthCurrentVO;
import com.eldercare.api.vo.ElderProfileVO;
import com.eldercare.api.vo.LoginVO;
import com.eldercare.api.vo.VolunteerBriefVO;
import com.eldercare.auth.entity.ElderProfile;
import com.eldercare.auth.entity.UserAccount;
import com.eldercare.auth.entity.UserRoleEntity;
import com.eldercare.auth.enums.AuthStatusEnum;
import com.eldercare.auth.mapper.ElderProfileMapper;
import com.eldercare.auth.mapper.UserAccountMapper;
import com.eldercare.auth.mapper.UserRoleMapper;
import com.eldercare.auth.service.AuthAppService;
import com.eldercare.auth.wechat.WechatCode2SessionClient;
import com.eldercare.auth.wechat.WechatPhoneNumber;
import com.eldercare.auth.wechat.WechatPhoneNumberClient;
import com.eldercare.auth.wechat.WechatSession;
import com.eldercare.common.enums.RoleEnum;
import com.eldercare.common.context.LoginUser;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.context.UserInfoDTO;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.common.jwt.JwtUtil;
import com.eldercare.common.log.annotation.OperationLog;
import com.eldercare.common.log.enums.OperationModuleEnum;
import com.eldercare.common.log.enums.OperationTypeEnum;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 认证服务业务实现。
 */
@Service
public class AuthAppServiceImpl implements AuthAppService {
    /** 用户账号 Mapper。 */
    private final UserAccountMapper userAccountMapper;
    /** 用户角色 Mapper。 */
    private final UserRoleMapper userRoleMapper;
    /** 老人档案 Mapper。 */
    private final ElderProfileMapper elderProfileMapper;
    /** JWT 工具。 */
    private final JwtUtil jwtUtil;
    /** 微信登录客户端。 */
    private final WechatCode2SessionClient wechatCode2SessionClient;
    /** 微信手机号客户端。 */
    private final WechatPhoneNumberClient wechatPhoneNumberClient;
    /** Redis 客户端。 */
    private final StringRedisTemplate redisTemplate;

    public AuthAppServiceImpl(UserAccountMapper userAccountMapper,
                              UserRoleMapper userRoleMapper,
                              ElderProfileMapper elderProfileMapper,
                              JwtUtil jwtUtil,
                              WechatCode2SessionClient wechatCode2SessionClient,
                              WechatPhoneNumberClient wechatPhoneNumberClient,
                              StringRedisTemplate redisTemplate) {
        // 保存账号 Mapper。
        this.userAccountMapper = userAccountMapper;
        // 保存角色 Mapper。
        this.userRoleMapper = userRoleMapper;
        // 保存老人档案 Mapper。
        this.elderProfileMapper = elderProfileMapper;
        // 保存 JWT 工具。
        this.jwtUtil = jwtUtil;
        // 保存微信登录客户端。
        this.wechatCode2SessionClient = wechatCode2SessionClient;
        // 保存微信手机号客户端。
        this.wechatPhoneNumberClient = wechatPhoneNumberClient;
        // 保存 Redis 客户端。
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = OperationModuleEnum.AUTH, operationType = OperationTypeEnum.LOGIN,
            description = "微信小程序登录", recordResult = false)
    public LoginVO wxLogin(MiniAppLoginDTO loginDTO) {
        // 社区 ID 不传时使用演示社区。
        Long communityId = loginDTO.communityId() == null ? AuthStatusEnum.DEFAULT_COMMUNITY.longCode() : loginDTO.communityId();
        // 调用微信 code2session 获取 openId。
        WechatSession session = wechatCode2SessionClient.code2Session(loginDTO.code());
        // 按 openId 查询账号。
        UserAccount account = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getOpenId, session.openId())
                .eq(UserAccount::getDeleted, 0)
                .last("LIMIT 1"));
        // 标记是否新账号。
        boolean created = account == null;
        // 首次登录自动注册账号。
        if (created) {
            // 创建新账号。
            account = new UserAccount();
            // 写入社区。
            account.setCommunityId(communityId);
            // 写入 openId。
            account.setOpenId(session.openId());
            // 写入 unionId。
            account.setUnionId(session.unionId());
            // 默认昵称。
            account.setNickname("微信用户");
            // 未知性别。
            account.setGender(0);
            // 正常状态。
            account.setAccountStatus(AuthStatusEnum.ACCOUNT_NORMAL.code());
            // 初始刷新版本。
            account.setRefreshTokenVersion(0);
            // 未删除。
            account.setDeleted(0);
            // 插入账号。
            userAccountMapper.insert(account);
        } else {
            // 社区级平台不允许账号跨社区静默登录。
            if (!Objects.equals(account.getCommunityId(), communityId)) {
                // 抛出跨社区异常。
                throw new BizException(ErrorCode.FORBIDDEN, "该微信账号已属于其他社区");
            }
            // 校验账号启用。
            ensureAccountEnabled(account);
            // 微信后续返回 unionId 时补写。
            if (StringUtils.hasText(session.unionId()) && !Objects.equals(account.getUnionId(), session.unionId())) {
                // 更新 unionId。
                account.setUnionId(session.unionId());
            }
        }
        // 解析当前登录角色。
        List<String> roles = resolveLoginRoles(communityId, account.getId(), loginDTO.loginRole(), loginDTO.roles(), created);
        // 保存当前角色。
        account.setCurrentRole(roles.get(0));
        // 更新最近登录时间。
        account.setLastLoginTime(LocalDateTime.now());
        // 更新账号。
        userAccountMapper.updateById(account);
        // 签发登录结果。
        return issueLoginVO(account, communityId, roles);
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 从 Redis 查询 refreshToken。
        String value = redisTemplate.opsForValue().get(refreshKey(refreshToken));
        // Redis 中没有说明已过期或已退出。
        if (!StringUtils.hasText(value)) {
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
        // 格式：userId:communityId:role:version。
        String[] parts = value.split(":");
        // 格式错误时拒绝。
        if (parts.length != 4) {
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "刷新令牌格式错误");
        }
        // 解析用户 ID。
        Long userId = Long.valueOf(parts[0]);
        // 解析社区 ID。
        Long communityId = Long.valueOf(parts[1]);
        // 解析当前角色。
        String role = parts[2];
        // 解析刷新版本。
        Integer version = Integer.valueOf(parts[3]);
        // 查询账号。
        UserAccount account = selectAccountInCommunity(communityId, userId);
        // 账号不存在时拒绝。
        if (account == null) {
            // 抛出未登录。
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        // 校验账号启用。
        ensureAccountEnabled(account);
        // 版本不一致说明已退出或被强制下线。
        if (!Objects.equals(account.getRefreshTokenVersion(), version)) {
            // 删除旧 refreshToken。
            redisTemplate.delete(refreshKey(refreshToken));
            // 抛出未登录。
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        // 查询账号角色。
        List<String> roles = listRoleCodes(communityId, userId);
        // 当前角色排第一位。
        roles = orderRolesByCurrentRole(roles, role);
        // 删除旧 refreshToken，实现旋转。
        redisTemplate.delete(refreshKey(refreshToken));
        // 重新签发。
        return issueLoginVO(account, communityId, roles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = OperationModuleEnum.AUTH, operationType = OperationTypeEnum.LOGOUT,
            description = "退出登录", recordParams = false)
    public void logout(String refreshToken) {
        // 读取当前用户。
        UserInfoDTO user = loadRequiredUser();
        // 查询账号。
        UserAccount account = selectAccountInCommunity(user.communityId(), user.userId());
        // 账号不存在时拒绝。
        if (account == null) {
            // 抛出未登录。
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        // 递增刷新版本，让历史 refreshToken 失效。
        account.setRefreshTokenVersion((account.getRefreshTokenVersion() == null ? 0 : account.getRefreshTokenVersion()) + 1);
        // 更新账号。
        userAccountMapper.updateById(account);
        // 删除当前 refreshToken。
        if (StringUtils.hasText(refreshToken)) {
            // 删除 Redis key。
            redisTemplate.delete(refreshKey(refreshToken));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = OperationModuleEnum.USER, operationType = OperationTypeEnum.UPDATE,
            description = "绑定手机号", recordParams = false)
    public void bindPhone(PhoneBindDTO bindDTO) {
        // 读取当前用户。
        UserInfoDTO user = loadRequiredUser();
        // 查询账号。
        UserAccount account = selectAccountInCommunity(user.communityId(), user.userId());
        // 账号不存在时拒绝。
        if (account == null) {
            // 抛出未登录。
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        // 查询手机号是否已被其他账号占用。
        UserAccount existed = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getPhone, bindDTO.phone())
                .eq(UserAccount::getDeleted, 0)
                .last("LIMIT 1"));
        // 被其他账号占用时拒绝。
        if (existed != null && !Objects.equals(existed.getId(), account.getId())) {
            // 抛出重复异常。
            throw new BizException(ErrorCode.DATA_EXISTS, "手机号已绑定其他账号");
        }
        // 绑定手机号。
        account.setPhone(bindDTO.phone());
        // 更新账号。
        userAccountMapper.updateById(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = OperationModuleEnum.USER, operationType = OperationTypeEnum.UPDATE,
            description = "绑定微信手机号", recordParams = false)
    public void bindWechatPhone(WechatPhoneBindDTO bindDTO) {
        // 调用微信手机号接口。
        WechatPhoneNumber phoneNumber = wechatPhoneNumberClient.getPhoneNumber(bindDTO.code());
        // 复用手机号绑定逻辑。
        bindPhone(new PhoneBindDTO(phoneNumber.purePhoneNumber()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = OperationModuleEnum.AUTH, operationType = OperationTypeEnum.UPDATE,
            description = "切换登录身份", bizId = "#roleDTO.targetRole()", recordResult = false)
    public LoginVO switchRole(SwitchRoleDTO roleDTO) {
        // 读取当前用户。
        UserInfoDTO user = loadRequiredUser();
        // 切换社区为空时沿用当前社区。
        Long communityId = roleDTO.communityId() == null ? user.communityId() : roleDTO.communityId();
        // 查询账号。
        UserAccount account = selectAccountInCommunity(communityId, user.userId());
        // 账号不存在时拒绝。
        if (account == null) {
            // 抛出未登录。
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        // 校验账号启用。
        ensureAccountEnabled(account);
        // 查询当前社区角色。
        List<String> roles = listRoleCodes(communityId, user.userId());
        // 解析目标角色。
        String targetRole = normalizeRole(roleDTO.targetRole());
        // 未拥有角色时拒绝。
        if (!roles.contains(targetRole)) {
            // 抛出无权限。
            throw new BizException(ErrorCode.FORBIDDEN, "账号没有该角色");
        }
        // 写入当前角色。
        account.setCurrentRole(targetRole);
        // 更新账号。
        userAccountMapper.updateById(account);
        // 目标角色排第一。
        return issueLoginVO(account, communityId, orderRolesByCurrentRole(roles, targetRole));
    }

    @Override
    public AuthCurrentVO current() {
        // 读取当前用户。
        UserInfoDTO user = loadRequiredUser();
        // 查询账号。
        UserAccount account = selectAccountInCommunity(user.communityId(), user.userId());
        // 账号不存在时拒绝。
        if (account == null) {
            // 抛出未登录。
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        // 查询角色列表。
        List<String> roles = listRoleCodes(user.communityId(), user.userId());
        // 查询老人档案。
        ElderProfile elderProfile = selectElderProfile(user.communityId(), user.userId());
        // 志愿者摘要。
        VolunteerBriefVO volunteer = roles.contains(RoleEnum.VOLUNTEER.code()) ? new VolunteerBriefVO(user.userId(), account.getNickname(), account.getAvatarUrl()) : null;
        // 返回当前认证用户。
        return new AuthCurrentVO(account.getId(), account.getNickname(), account.getPhone(), account.getAvatarUrl(),
                StringUtils.hasText(account.getCurrentRole()) ? account.getCurrentRole() : (roles.isEmpty() ? null : roles.get(0)),
                account.getCommunityId(), roles, elderProfile == null ? null : toElderProfileVO(elderProfile), volunteer);
    }

    /** 解析登录角色。 */
    private List<String> resolveLoginRoles(Long communityId, Long userId, String loginRole, List<String> requestRoles, boolean newAccount) {
        // 查询已有角色。
        List<String> existedRoles = listRoleCodes(communityId, userId);
        // 新账号没有角色时初始化一个角色。
        if (existedRoles.isEmpty()) {
            // 优先 loginRole，其次 roles，最后默认老人。
            List<String> initialRoles = StringUtils.hasText(loginRole) ? List.of(normalizeRole(loginRole)) : normalizeRoles(requestRoles);
            // 写入初始角色。
            initialRoles.forEach(role -> ensureRole(communityId, userId, role));
            // 返回初始角色。
            return initialRoles;
        }
        // 指定 loginRole 时必须已拥有。
        if (StringUtils.hasText(loginRole)) {
            // 规范化角色。
            String targetRole = normalizeRole(loginRole);
            // 未拥有时拒绝。
            if (!existedRoles.contains(targetRole)) {
                // 防止前端自行提权。
                throw new BizException(ErrorCode.FORBIDDEN, "账号没有该登录角色");
            }
            // 目标角色排第一。
            return orderRolesByCurrentRole(existedRoles, targetRole);
        }
        // 兼容 roles 字段，只用于选择已拥有角色。
        if (requestRoles != null && !requestRoles.isEmpty()) {
            // 解析请求角色。
            List<String> requested = normalizeRoles(requestRoles);
            // 找到第一个已拥有角色。
            return requested.stream().filter(existedRoles::contains).findFirst()
                    .map(role -> orderRolesByCurrentRole(existedRoles, role))
                    .orElseThrow(() -> new BizException(ErrorCode.FORBIDDEN, "账号没有请求的登录角色"));
        }
        // 默认已有角色第一个为当前角色。
        return orderRolesByCurrentRole(existedRoles, existedRoles.get(0));
    }

    /** 确保用户拥有角色。 */
    private void ensureRole(Long communityId, Long userId, String roleCode) {
        // 规范化角色。
        String normalizedRole = normalizeRole(roleCode);
        // 查询是否已有角色。
        UserRoleEntity existed = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getCommunityId, communityId)
                .eq(UserRoleEntity::getUserId, userId)
                .eq(UserRoleEntity::getRoleCode, normalizedRole)
                .eq(UserRoleEntity::getDeleted, 0)
                .last("LIMIT 1"));
        // 已存在时确保启用。
        if (existed != null) {
            // 状态非正常时恢复。
            if (!Objects.equals(existed.getRoleStatus(), AuthStatusEnum.ACCOUNT_NORMAL.code())) {
                // 设置正常状态。
                existed.setRoleStatus(AuthStatusEnum.ACCOUNT_NORMAL.code());
                // 更新角色。
                userRoleMapper.updateById(existed);
            }
            // 结束。
            return;
        }
        // 创建角色。
        UserRoleEntity role = new UserRoleEntity();
        // 写入社区。
        role.setCommunityId(communityId);
        // 写入用户。
        role.setUserId(userId);
        // 写入角色。
        role.setRoleCode(normalizedRole);
        // 正常状态。
        role.setRoleStatus(AuthStatusEnum.ACCOUNT_NORMAL.code());
        // 未删除。
        role.setDeleted(0);
        // 插入角色。
        userRoleMapper.insert(role);
    }

    /** 查询指定社区账号。 */
    private UserAccount selectAccountInCommunity(Long communityId, Long userId) {
        // 使用 community_id 做数据隔离。
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getCommunityId, communityId)
                .eq(UserAccount::getId, userId)
                .eq(UserAccount::getDeleted, 0)
                .last("LIMIT 1"));
    }

    /** 查询角色编码。 */
    private List<String> listRoleCodes(Long communityId, Long userId) {
        // 查询正常角色。
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getCommunityId, communityId)
                        .eq(UserRoleEntity::getUserId, userId)
                        .eq(UserRoleEntity::getRoleStatus, AuthStatusEnum.ACCOUNT_NORMAL.code())
                        .eq(UserRoleEntity::getDeleted, 0))
                .stream()
                .map(UserRoleEntity::getRoleCode)
                .toList();
    }

    /** 角色列表按当前角色排序。 */
    private List<String> orderRolesByCurrentRole(List<String> roles, String currentRole) {
        // 当前角色无效时保持原列表。
        if (!StringUtils.hasText(currentRole) || roles == null || !roles.contains(currentRole)) {
            // 返回原列表。
            return roles == null ? List.of() : roles;
        }
        // 当前角色排第一。
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(currentRole), roles.stream().filter(role -> !role.equals(currentRole))).toList();
    }

    /** 签发登录响应。 */
    private LoginVO issueLoginVO(UserAccount account, Long communityId, List<String> roles) {
        // 校验账号状态。
        ensureAccountEnabled(account);
        // 没有角色时拒绝。
        if (roles == null || roles.isEmpty()) {
            // 抛出无权限。
            throw new BizException(ErrorCode.FORBIDDEN, "账号没有可用角色");
        }
        // 构造 JWT 载荷。
        LoginUser loginUser = new LoginUser(account.getId(), communityId, roles, account.getPhone());
        // 生成 accessToken。
        String accessToken = jwtUtil.generateToken(loginUser);
        // 生成 refreshToken。
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        // 当前刷新版本。
        Integer refreshVersion = account.getRefreshTokenVersion() == null ? 0 : account.getRefreshTokenVersion();
        // 写入 Redis，格式 userId:communityId:role:version。
        redisTemplate.opsForValue().set(refreshKey(refreshToken), account.getId() + ":" + communityId + ":" + roles.get(0) + ":" + refreshVersion, Duration.ofDays(30));
        // 是否绑定手机号。
        boolean phoneBound = StringUtils.hasText(account.getPhone());
        // 返回登录结果。
        return new LoginVO(account.getId(), loginUser.role(), accessToken, refreshToken, roles, communityId, phoneBound);
    }

    /** 账号是否启用。 */
    private void ensureAccountEnabled(UserAccount account) {
        // 禁用账号不能登录。
        if (account == null || Objects.equals(account.getAccountStatus(), AuthStatusEnum.ACCOUNT_DISABLED.code())) {
            // 抛出禁用异常。
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
    }

    /** refreshToken Redis key。 */
    private String refreshKey(String refreshToken) {
        // 固定前缀方便排查。
        return "auth:refresh:" + refreshToken;
    }

    /** 读取当前用户。 */
    private UserInfoDTO loadRequiredUser() {
        // 从请求头加载用户上下文。
        UserContext.loadFromCurrentRequest();
        // 校验登录。
        UserContext.requireLoginUser();
        // 校验社区。
        UserContext.requireCommunityId();
        // 返回上下文。
        return UserContext.get();
    }

    /** 查询老人档案。 */
    private ElderProfile selectElderProfile(Long communityId, Long elderUserId) {
        // 使用 community_id 防止跨社区。
        return elderProfileMapper.selectOne(new LambdaQueryWrapper<ElderProfile>()
                .eq(ElderProfile::getCommunityId, communityId)
                .eq(ElderProfile::getUserId, elderUserId)
                .eq(ElderProfile::getDeleted, 0)
                .last("LIMIT 1"));
    }

    /** 转换老人档案。 */
    private ElderProfileVO toElderProfileVO(ElderProfile elderProfile) {
        // 组装 VO。
        return new ElderProfileVO(elderProfile.getId(), elderProfile.getUserId(), elderProfile.getElderName(), elderProfile.getElderPhone(), elderProfile.getAge(), elderProfile.getAddress(), elderProfile.getHealthNote());
    }

    /** 规范化角色列表。 */
    private List<String> normalizeRoles(List<String> roles) {
        // 未传时默认老人。
        if (roles == null || roles.isEmpty()) {
            // 返回默认角色。
            return List.of(RoleEnum.ELDER.code());
        }
        // 过滤空白并去重。
        List<String> cleaned = roles.stream().filter(StringUtils::hasText).map(this::normalizeRole).distinct().toList();
        // 清理后为空时默认老人。
        return cleaned.isEmpty() ? List.of(RoleEnum.ELDER.code()) : cleaned;
    }

    /** 规范化单个角色。 */
    private String normalizeRole(String roleCode) {
        // 角色不能为空。
        if (!StringUtils.hasText(roleCode)) {
            // 抛出参数异常。
            throw new BizException(ErrorCode.PARAM_ERROR, "角色不能为空");
        }
        // 转成大写。
        String normalized = roleCode.trim().toUpperCase();
        // 校验是否允许。
        if (!RoleEnum.codes().contains(normalized)) {
            // 抛出参数异常。
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的角色：" + roleCode);
        }
        // 返回规范角色。
        return normalized;
    }
}
