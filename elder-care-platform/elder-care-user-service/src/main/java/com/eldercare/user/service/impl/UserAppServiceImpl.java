package com.eldercare.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.api.dto.*;
import com.eldercare.api.vo.AuthCurrentVO;
import com.eldercare.api.vo.ElderProfileVO;
import com.eldercare.api.vo.LoginVO;
import com.eldercare.api.vo.UserInfoVO;
import com.eldercare.api.vo.VolunteerBriefVO;
import com.eldercare.common.constant.RoleConstants;
import com.eldercare.common.context.LoginUser;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.context.UserInfoDTO;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import com.eldercare.common.jwt.JwtUtil;
import com.eldercare.user.entity.ElderProfile;
import com.eldercare.user.entity.FamilyElderBind;
import com.eldercare.user.entity.UserAccount;
import com.eldercare.user.entity.UserRoleEntity;
import com.eldercare.user.mapper.ElderProfileMapper;
import com.eldercare.user.mapper.FamilyElderBindMapper;
import com.eldercare.user.mapper.UserAccountMapper;
import com.eldercare.user.mapper.UserRoleMapper;
import com.eldercare.user.service.UserAppService;
import com.eldercare.user.wechat.WechatCode2SessionClient;
import com.eldercare.user.wechat.WechatPhoneNumber;
import com.eldercare.user.wechat.WechatPhoneNumberClient;
import com.eldercare.user.wechat.WechatSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 用户服务业务实现。
 */
@Service
public class UserAppServiceImpl implements UserAppService {

    /**
     * 默认社区 ID，第一版模拟登录未选择社区时使用。
     */
    private static final Long DEFAULT_COMMUNITY_ID = 1L;

    /**
     * 正常状态。
     */
    private static final int STATUS_NORMAL = 1;

    /**
     * 禁用状态。
     */
    private static final int STATUS_DISABLED = 2;

    /**
     * 已绑定状态。
     */
    private static final int BIND_STATUS_BOUND = 2;

    /**
     * 系统允许的用户角色集合。
     */
    private static final Set<String> ALLOWED_ROLES = Set.of(RoleConstants.ELDER, RoleConstants.FAMILY, RoleConstants.VOLUNTEER, RoleConstants.ADMIN);

    /**
     * 用户账号 Mapper。
     */
    private final UserAccountMapper userAccountMapper;

    /**
     * 用户角色 Mapper。
     */
    private final UserRoleMapper userRoleMapper;

    /**
     * 老人档案 Mapper。
     */
    private final ElderProfileMapper elderProfileMapper;

    /**
     * 亲情号绑定 Mapper。
     */
    private final FamilyElderBindMapper familyElderBindMapper;

    /**
     * JWT 工具类。
     */
    private final JwtUtil jwtUtil;

    /**
     * 微信 code2session 客户端。
     */
    private final WechatCode2SessionClient wechatCode2SessionClient;

    /**
     * 微信手机号解析客户端。
     */
    private final WechatPhoneNumberClient wechatPhoneNumberClient;

    /**
     * Redis 客户端，用于保存 refresh token。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造业务服务。
     */
    public UserAppServiceImpl(UserAccountMapper userAccountMapper,
                              UserRoleMapper userRoleMapper,
                              ElderProfileMapper elderProfileMapper,
                              FamilyElderBindMapper familyElderBindMapper,
                              JwtUtil jwtUtil,
                              WechatCode2SessionClient wechatCode2SessionClient,
                              WechatPhoneNumberClient wechatPhoneNumberClient,
                              StringRedisTemplate stringRedisTemplate) {
        // 保存用户账号 Mapper。
        this.userAccountMapper = userAccountMapper;
        // 保存用户角色 Mapper。
        this.userRoleMapper = userRoleMapper;
        // 保存老人档案 Mapper。
        this.elderProfileMapper = elderProfileMapper;
        // 保存亲情号绑定 Mapper。
        this.familyElderBindMapper = familyElderBindMapper;
        // 保存 JWT 工具类。
        this.jwtUtil = jwtUtil;
        // 保存微信登录客户端。
        this.wechatCode2SessionClient = wechatCode2SessionClient;
        // 保存微信手机号解析客户端。
        this.wechatPhoneNumberClient = wechatPhoneNumberClient;
        // 保存 Redis 客户端。
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 模拟登录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO mockLogin(MockLoginDTO loginDTO) {
        // 社区 ID 不传时使用默认演示社区。
        Long communityId = loginDTO.communityId() == null ? DEFAULT_COMMUNITY_ID : loginDTO.communityId();
        // 根据 userId 或 phone 查询/创建账号。
        UserAccount account = resolveMockLoginAccount(loginDTO, communityId);
        // 解析模拟登录角色，不传时默认老人。
        List<String> roles = resolveRoles(loginDTO.roles());
        // lambda 中引用的账号 ID 必须是实际 final。
        Long accountId = account.getId();
        // 确保模拟账号拥有请求角色，方便本地联调多身份场景。
        roles.forEach(role -> ensureRole(communityId, accountId, role));
        // 当前角色使用角色列表第一个元素。
        account.setCurrentRole(roles.get(0));
        // 更新最近登录时间。
        account.setLastLoginTime(LocalDateTime.now());
        // 持久化最近登录时间。
        userAccountMapper.updateById(account);
        // 生成 JWT 登录用户载荷。
        LoginUser loginUser = new LoginUser(account.getId(), communityId, roles, account.getPhone());
        // 返回登录结果，主角色使用第一个角色。
        return issueLoginVO(account, communityId, roles);
    }

    /**
     * 微信小程序登录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO miniAppLogin(MiniAppLoginDTO loginDTO) {
        // 社区 ID 不传时使用默认演示社区。
        Long communityId = loginDTO.communityId() == null ? DEFAULT_COMMUNITY_ID : loginDTO.communityId();
        // 调用微信 code2session，真实环境走微信接口，本地未配置时走 mock。
        WechatSession session = wechatCode2SessionClient.code2Session(loginDTO.code());
        // 按 openId 查询小程序账号。
        UserAccount account = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getOpenId, session.openId())
                .eq(UserAccount::getDeleted, 0)
                .last("LIMIT 1"));
        // 标记是否为首次注册账号。
        boolean created = account == null;
        // 首次登录时自动注册账号。
        if (account == null) {
            // 创建新的小程序账号。
            account = new UserAccount();
            // 写入当前社区。
            account.setCommunityId(communityId);
            // 写入微信 openId。
            account.setOpenId(session.openId());
            // 写入 unionId，可能为空。
            account.setUnionId(session.unionId());
            // 设置默认昵称。
            account.setNickname("微信用户");
            // 设置未知性别。
            account.setGender(0);
            // 设置正常状态。
            account.setAccountStatus(STATUS_NORMAL);
            // 初始化刷新令牌版本。
            account.setRefreshTokenVersion(0);
            // 设置未删除。
            account.setDeleted(0);
            // 插入账号。
            userAccountMapper.insert(account);
        } else {
            // 已有账号但社区不一致时，社区级平台不允许静默跨社区登录。
            if (!Objects.equals(account.getCommunityId(), communityId)) {
                // 抛出跨社区错误。
                throw new BizException(ErrorCode.FORBIDDEN, "该微信账号已属于其他社区");
            }
            // 禁用账号不能登录。
            ensureAccountEnabled(account);
            // 更新 unionId，兼容后续绑定开放平台后微信才返回 unionId 的情况。
            if (StringUtils.hasText(session.unionId()) && !Objects.equals(account.getUnionId(), session.unionId())) {
                // 写入新的 unionId。
                account.setUnionId(session.unionId());
            }
        }
        // 根据已有角色和本次 loginRole 解析当前登录身份。
        List<String> roles = resolveMiniAppRoles(communityId, account.getId(), loginDTO.loginRole(), loginDTO.roles(), created);
        // 当前角色使用第一个角色。
        account.setCurrentRole(roles.get(0));
        // 更新最近登录时间。
        account.setLastLoginTime(LocalDateTime.now());
        // 保存账号变更。
        userAccountMapper.updateById(account);
        // 签发登录结果。
        return issueLoginVO(account, communityId, roles);
    }

    /**
     * 刷新访问令牌。
     */
    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 根据 refresh token 查询 Redis 会话。
        String value = stringRedisTemplate.opsForValue().get(refreshKey(refreshToken));
        // Redis 中不存在说明 refresh token 过期或已退出。
        if (!StringUtils.hasText(value)) {
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
        // 格式：userId:communityId:role:version；兼容旧格式 userId:communityId:version。
        String[] parts = value.split(":");
        // 格式错误时拒绝刷新。
        if (parts.length != 3 && parts.length != 4) {
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "刷新令牌格式错误");
        }
        // 解析用户 ID。
        Long userId = Long.valueOf(parts[0]);
        // 解析社区 ID。
        Long communityId = Long.valueOf(parts[1]);
        // 解析刷新令牌携带的当前角色。
        String refreshRole = parts.length == 4 ? parts[2] : null;
        // 解析刷新令牌版本。
        Integer version = Integer.valueOf(parts.length == 4 ? parts[3] : parts[2]);
        // 查询当前账号。
        UserAccount account = selectAccountInCommunity(communityId, userId);
        // 账号不存在时拒绝刷新。
        if (account == null) {
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        // 禁用账号不能刷新 token。
        ensureAccountEnabled(account);
        // 账号版本不一致说明已经退出或被强制下线。
        if (!Objects.equals(account.getRefreshTokenVersion(), version)) {
            // 删除旧 refresh token。
            stringRedisTemplate.delete(refreshKey(refreshToken));
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        // 查询当前账号启用角色。
        List<String> roles = listRoleCodes(communityId, userId);
        // 按 refresh token 中的角色优先调整角色顺序，保证刷新后身份不漂移。
        roles = orderRolesByCurrentRole(roles, StringUtils.hasText(refreshRole) ? refreshRole : account.getCurrentRole());
        // 没有角色时拒绝刷新。
        if (roles.isEmpty()) {
            // 抛出无权限异常。
            throw new BizException(ErrorCode.FORBIDDEN, "账号没有可用角色");
        }
        // 旧 refresh token 用完即删，降低泄露后可重复使用的风险。
        stringRedisTemplate.delete(refreshKey(refreshToken));
        // 重新签发访问令牌和刷新令牌。
        return issueLoginVO(account, communityId, roles);
    }

    /**
     * 退出登录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout() {
        // 兼容第一版无参退出接口。
        logout(null);
    }

    /**
     * 退出登录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(String refreshToken) {
        // 读取当前用户上下文。
        UserInfoDTO userInfo = loadRequiredUser();
        // 查询当前账号。
        UserAccount account = selectAccountInCommunity(userInfo.communityId(), userInfo.userId());
        // 账号不存在时无需继续。
        if (account == null) {
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        // 递增刷新令牌版本，让该账号之前签发的 refresh token 全部失效。
        account.setRefreshTokenVersion((account.getRefreshTokenVersion() == null ? 0 : account.getRefreshTokenVersion()) + 1);
        // 更新账号。
        userAccountMapper.updateById(account);
        // 如果客户端传了当前 refresh token，则立即删除该 Redis 会话。
        if (StringUtils.hasText(refreshToken)) {
            // 删除 Redis 中的 refresh token key。
            stringRedisTemplate.delete(refreshKey(refreshToken));
        }
    }

    /**
     * 绑定手机号。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPhone(PhoneBindDTO bindDTO) {
        // 读取当前用户上下文。
        UserInfoDTO userInfo = loadRequiredUser();
        // 查询当前账号。
        UserAccount account = selectAccountInCommunity(userInfo.communityId(), userInfo.userId());
        // 账号不存在时拒绝绑定。
        if (account == null) {
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        // 查询手机号是否已被其他账号使用。
        UserAccount existed = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getPhone, bindDTO.phone())
                .eq(UserAccount::getDeleted, 0)
                .last("LIMIT 1"));
        // 手机号已被其他账号绑定时拒绝。
        if (existed != null && !Objects.equals(existed.getId(), account.getId())) {
            // 抛出重复异常。
            throw new BizException(ErrorCode.DATA_EXISTS, "手机号已绑定其他账号");
        }
        // 绑定手机号。
        account.setPhone(bindDTO.phone());
        // 更新账号。
        userAccountMapper.updateById(account);
    }

    /**
     * 使用微信手机号凭证绑定手机号。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindWechatPhone(WechatPhoneBindDTO bindDTO) {
        // 调用微信接口或本地 mock 获取可信手机号。
        WechatPhoneNumber phoneNumber = wechatPhoneNumberClient.getPhoneNumber(bindDTO.code());
        // 复用统一手机号绑定逻辑，避免重复校验。
        bindPhone(new PhoneBindDTO(phoneNumber.purePhoneNumber()));
    }

    /**
     * 切换当前角色。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO switchRole(SwitchRoleDTO roleDTO) {
        // 读取当前用户上下文。
        UserInfoDTO userInfo = loadRequiredUser();
        // 切换社区为空时沿用当前 Token 中的社区。
        Long targetCommunityId = roleDTO.communityId() == null ? userInfo.communityId() : roleDTO.communityId();
        // 查询账号。
        UserAccount account = selectAccountInCommunity(targetCommunityId, userInfo.userId());
        // 账号不存在时拒绝切换。
        if (account == null) {
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        // 禁用账号不能切换角色。
        ensureAccountEnabled(account);
        // 当前可用角色列表。
        List<String> roles = listRoleCodes(targetCommunityId, userInfo.userId());
        // 解析目标角色。
        String targetRole = roleDTO.targetRole();
        // 目标角色必须已拥有。
        if (!roles.contains(targetRole)) {
            // 抛出无权限异常。
            throw new BizException(ErrorCode.FORBIDDEN, "账号没有该角色");
        }
        // 保存当前角色。
        account.setCurrentRole(targetRole);
        // 更新账号。
        userAccountMapper.updateById(account);
        // 让目标角色排在第一位。
        List<String> orderedRoles = java.util.stream.Stream.concat(java.util.stream.Stream.of(targetRole), roles.stream().filter(role -> !role.equals(targetRole))).toList();
        // 重新签发 Token。
        return issueLoginVO(account, targetCommunityId, orderedRoles);
    }

    /**
     * 查询认证维度当前用户。
     */
    @Override
    public AuthCurrentVO currentAuth() {
        // 从请求头读取当前登录用户。
        UserInfoDTO userInfo = loadRequiredUser();
        // 查询当前账号。
        UserAccount account = selectAccountInCommunity(userInfo.communityId(), userInfo.userId());
        // Token 中的用户不存在时拒绝访问。
        if (account == null) {
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        // 查询账号角色列表。
        List<String> roles = listRoleCodes(userInfo.communityId(), userInfo.userId());
        // 查询老人档案摘要。
        ElderProfile elderProfile = selectElderProfile(userInfo.communityId(), userInfo.userId());
        // 当前用户暂不跨库查询志愿者详细档案，只返回基础志愿者摘要。
        VolunteerBriefVO volunteer = roles.contains(RoleConstants.VOLUNTEER) ? new VolunteerBriefVO(userInfo.userId(), account.getNickname(), account.getAvatarUrl()) : null;
        // 组装认证信息响应。
        return new AuthCurrentVO(account.getId(), account.getNickname(), account.getPhone(), account.getAvatarUrl(),
                StringUtils.hasText(account.getCurrentRole()) ? account.getCurrentRole() : (roles.isEmpty() ? null : roles.get(0)),
                account.getCommunityId(), roles, elderProfile == null ? null : toElderProfileVO(elderProfile), volunteer);
    }

    /**
     * 查询当前登录用户。
     */
    @Override
    public UserInfoVO getCurrentUser() {
        // 从请求头加载并校验当前用户。
        UserInfoDTO userInfo = loadRequiredUser();
        // 查询当前用户账号。
        UserAccount account = selectAccountInCommunity(userInfo.communityId(), userInfo.userId());
        // 当前账号不存在时说明 Token 已经不再有效。
        if (account == null) {
            // 抛出未登录异常。
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        // 查询数据库中的角色，避免只相信 Token 中的角色。
        List<String> roles = listRoleCodes(userInfo.communityId(), userInfo.userId());
        // 返回当前用户信息。
        return new UserInfoVO(account.getId(), account.getCommunityId(), account.getPhone(), account.getNickname(), roles);
    }

    /**
     * 新增老人资料。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ElderProfileVO createElder(ElderCreateDTO createDTO) {
        // 从请求头读取社区上下文。
        UserInfoDTO userInfo = loadRequiredUser();
        // 解析或创建老人账号。
        UserAccount elderAccount = resolveElderAccount(userInfo.communityId(), createDTO);
        // 确保老人账号有 ELDER 角色。
        ensureRole(userInfo.communityId(), elderAccount.getId(), RoleConstants.ELDER);
        // 检查老人档案是否已存在。
        ElderProfile existed = selectElderProfile(userInfo.communityId(), elderAccount.getId());
        // 已存在时不允许重复创建。
        if (existed != null) {
            // 抛出数据已存在异常。
            throw new BizException(ErrorCode.DATA_EXISTS, "老人资料已存在");
        }
        // 构造老人档案。
        ElderProfile elderProfile = new ElderProfile();
        // 设置社区 ID。
        elderProfile.setCommunityId(userInfo.communityId());
        // 设置老人用户 ID。
        elderProfile.setUserId(elderAccount.getId());
        // 设置老人姓名。
        elderProfile.setElderName(createDTO.elderName());
        // 设置老人手机号。
        elderProfile.setElderPhone(StringUtils.hasText(createDTO.phone()) ? createDTO.phone() : elderAccount.getPhone());
        // 设置年龄。
        elderProfile.setAge(createDTO.age());
        // 设置住址。
        elderProfile.setAddress(createDTO.address());
        // 设置健康备注。
        elderProfile.setHealthNote(createDTO.healthNote());
        // 设置紧急联系人姓名。
        elderProfile.setEmergencyContactName(createDTO.emergencyContactName());
        // 设置紧急联系人手机号。
        elderProfile.setEmergencyContactPhone(createDTO.emergencyContactPhone());
        // 设置档案正常状态。
        elderProfile.setProfileStatus(STATUS_NORMAL);
        // 设置逻辑删除标记。
        elderProfile.setDeleted(0);
        // 插入老人档案。
        elderProfileMapper.insert(elderProfile);
        // 返回老人档案 VO。
        return toElderProfileVO(elderProfile);
    }

    /**
     * 亲情号绑定老人。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindElder(FamilyBindDTO bindDTO) {
        // 从请求头读取当前亲情号用户。
        UserInfoDTO userInfo = loadRequiredUser();
        // 确保当前用户拥有亲情号角色。
        ensureRole(userInfo.communityId(), userInfo.userId(), RoleConstants.FAMILY);
        // 只能绑定同社区内已建档老人。
        ElderProfile elderProfile = selectElderProfile(userInfo.communityId(), bindDTO.elderUserId());
        // 老人不存在说明跨社区或未建档。
        if (elderProfile == null) {
            // 抛出资源不存在异常。
            throw new BizException(ErrorCode.NOT_FOUND, "老人资料不存在或不属于当前社区");
        }
        // 查询是否已有绑定关系。
        FamilyElderBind existed = selectFamilyBind(userInfo.communityId(), userInfo.userId(), bindDTO.elderUserId());
        // 已经绑定时直接报重复。
        if (existed != null && Objects.equals(existed.getBindStatus(), BIND_STATUS_BOUND)) {
            // 抛出数据已存在异常。
            throw new BizException(ErrorCode.DATA_EXISTS, "亲情号已绑定该老人");
        }
        // 构造绑定记录。
        FamilyElderBind bind = existed == null ? new FamilyElderBind() : existed;
        // 设置社区 ID。
        bind.setCommunityId(userInfo.communityId());
        // 设置亲情号用户 ID。
        bind.setFamilyUserId(userInfo.userId());
        // 设置老人用户 ID。
        bind.setElderUserId(bindDTO.elderUserId());
        // 设置关系，默认 OTHER。
        bind.setRelationship(StringUtils.hasText(bindDTO.relationship()) ? bindDTO.relationship() : "OTHER");
        // 第一版模拟直接绑定成功。
        bind.setBindStatus(BIND_STATUS_BOUND);
        // 设置绑定来源。
        bind.setBindSource("MINI_APP");
        // 设置确认时间。
        bind.setConfirmedAt(LocalDateTime.now());
        // 设置未删除。
        bind.setDeleted(0);
        // 新记录插入，旧记录更新。
        if (bind.getId() == null) {
            // 插入绑定记录。
            familyElderBindMapper.insert(bind);
        } else {
            // 更新历史绑定记录。
            familyElderBindMapper.updateById(bind);
        }
    }

    /**
     * 查询当前亲情号绑定老人列表。
     */
    @Override
    public List<ElderProfileVO> listBoundElders() {
        // 从请求头读取当前用户。
        UserInfoDTO userInfo = loadRequiredUser();
        // 查询当前亲情号已绑定的老人 ID。
        List<FamilyElderBind> binds = familyElderBindMapper.selectList(new LambdaQueryWrapper<FamilyElderBind>()
                .eq(FamilyElderBind::getCommunityId, userInfo.communityId())
                .eq(FamilyElderBind::getFamilyUserId, userInfo.userId())
                .eq(FamilyElderBind::getBindStatus, BIND_STATUS_BOUND)
                .eq(FamilyElderBind::getDeleted, 0));
        // 根据绑定关系查询老人档案。
        return binds.stream()
                .map(bind -> selectElderProfile(userInfo.communityId(), bind.getElderUserId()))
                .filter(Objects::nonNull)
                .map(this::toElderProfileVO)
                .toList();
    }

    /**
     * 校验亲情号是否绑定老人。
     */
    @Override
    public boolean checkFamilyBind(Long communityId, Long familyUserId, Long elderUserId) {
        // 参数缺失时直接返回 false。
        if (communityId == null || familyUserId == null || elderUserId == null) {
            // 不允许操作。
            return false;
        }
        // 查询绑定记录是否存在。
        return selectFamilyBind(communityId, familyUserId, elderUserId) != null;
    }

    /**
     * 判断用户是否存在。
     */
    @Override
    public boolean existsById(Long userId) {
        // 空 ID 直接不存在。
        if (userId == null) {
            // 不存在。
            return false;
        }
        // 查询未删除用户。
        Long count = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getId, userId)
                .eq(UserAccount::getDeleted, 0));
        // 大于 0 表示存在。
        return count != null && count > 0;
    }

    /**
     * 从请求头读取并校验用户上下文。
     */
    private UserInfoDTO loadRequiredUser() {
        // 从当前请求头加载用户上下文。
        UserContext.loadFromCurrentRequest();
        // 校验登录状态。
        UserContext.requireLoginUser();
        // 校验社区上下文。
        UserContext.requireCommunityId();
        // 返回当前用户上下文。
        return UserContext.get();
    }

    /**
     * 根据模拟登录入参查询或创建账号。
     */
    private UserAccount resolveMockLoginAccount(MockLoginDTO loginDTO, Long communityId) {
        // userId 优先，适合测试已有账号。
        if (loginDTO.userId() != null) {
            // 查询指定社区内用户。
            UserAccount account = selectAccountInCommunity(communityId, loginDTO.userId());
            // 不存在时直接报错，避免创建出调用方不预期的 ID。
            if (account == null) {
                // 抛出资源不存在异常。
                throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
            }
            // 禁用账号不能登录。
            ensureAccountEnabled(account);
            // 返回已有账号。
            return account;
        }
        // phone 为空时无法模拟登录。
        if (!StringUtils.hasText(loginDTO.phone())) {
            // 抛出参数错误异常。
            throw new BizException(ErrorCode.PARAM_ERROR, "userId 或 phone 必须传一个");
        }
        // 根据手机号查询账号。
        UserAccount account = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getPhone, loginDTO.phone())
                .eq(UserAccount::getDeleted, 0)
                .last("LIMIT 1"));
        // 已有账号直接返回。
        if (account != null) {
            // 如果账号不在当前社区，第一版不自动迁移。
            if (!Objects.equals(account.getCommunityId(), communityId)) {
                // 抛出跨社区错误。
                throw new BizException(ErrorCode.FORBIDDEN, "手机号已属于其他社区");
            }
            // 禁用账号不能登录。
            ensureAccountEnabled(account);
            // 返回已有账号。
            return account;
        }
        // 创建模拟账号。
        UserAccount newAccount = new UserAccount();
        // 设置社区 ID。
        newAccount.setCommunityId(communityId);
        // 设置手机号。
        newAccount.setPhone(loginDTO.phone());
        // 设置昵称。
        newAccount.setNickname("模拟用户" + loginDTO.phone());
        // 设置未知性别。
        newAccount.setGender(0);
        // 设置正常状态。
        newAccount.setAccountStatus(STATUS_NORMAL);
        // 初始化当前角色为空，登录流程稍后写入。
        newAccount.setCurrentRole(null);
        // 初始化刷新令牌版本。
        newAccount.setRefreshTokenVersion(0);
        // 设置未删除。
        newAccount.setDeleted(0);
        // 插入账号。
        userAccountMapper.insert(newAccount);
        // 返回新账号。
        return newAccount;
    }

    /**
     * 解析登录角色列表。
     */
    private List<String> resolveRoles(List<String> roles) {
        // 未传角色时默认老人角色。
        if (roles == null || roles.isEmpty()) {
            // 返回默认角色。
            return List.of(RoleConstants.ELDER);
        }
        // 去掉空白角色。
        List<String> cleaned = roles.stream().filter(StringUtils::hasText).map(String::trim).map(String::toUpperCase).distinct().toList();
        // 校验角色必须是系统允许的枚举值。
        cleaned.forEach(this::checkRoleCode);
        // 清理后为空也默认老人。
        return cleaned.isEmpty() ? List.of(RoleConstants.ELDER) : cleaned;
    }

    /**
     * 解析微信小程序登录角色。
     *
     * @param communityId 当前社区 ID。
     * @param userId      用户 ID。
     * @param loginRole   本次登录目标角色。
     * @param requestRoles 兼容第一版的角色列表。
     * @param newAccount  是否为新账号。
     * @return 当前角色排第一位的角色列表。
     */
    private List<String> resolveMiniAppRoles(Long communityId, Long userId, String loginRole, List<String> requestRoles, boolean newAccount) {
        // 先查询数据库中账号已有角色。
        List<String> existedRoles = listRoleCodes(communityId, userId);
        // 新账号没有任何角色时，使用 loginRole 或默认老人角色初始化。
        if (existedRoles.isEmpty()) {
            // 优先使用 loginRole，其次兼容 roles，第一个也没有则默认老人。
            List<String> initialRoles = StringUtils.hasText(loginRole) ? List.of(loginRole.trim().toUpperCase()) : resolveRoles(requestRoles);
            // 为新账号写入初始角色。
            initialRoles.forEach(role -> ensureRole(communityId, userId, role));
            // 返回初始化后的角色列表。
            return initialRoles;
        }
        // 已有账号如果传了 loginRole，必须校验是否已经拥有。
        if (StringUtils.hasText(loginRole)) {
            // 转大写对齐角色常量。
            String targetRole = loginRole.trim().toUpperCase();
            // 校验角色编码合法。
            checkRoleCode(targetRole);
            // 未拥有该角色时拒绝登录，防止前端自行提权。
            if (!existedRoles.contains(targetRole)) {
                // 抛出无权限异常。
                throw new BizException(ErrorCode.FORBIDDEN, "账号没有该登录角色");
            }
            // 目标角色排第一位。
            return orderRolesByCurrentRole(existedRoles, targetRole);
        }
        // 兼容第一版：已有账号传 roles 只用于选择当前身份，不再自动授予新角色。
        if (requestRoles != null && !requestRoles.isEmpty()) {
            // 解析并校验角色列表。
            List<String> requested = resolveRoles(requestRoles);
            // 找到第一个已拥有角色作为当前身份。
            return requested.stream().filter(existedRoles::contains).findFirst()
                    .map(role -> orderRolesByCurrentRole(existedRoles, role))
                    .orElseThrow(() -> new BizException(ErrorCode.FORBIDDEN, "账号没有请求的登录角色"));
        }
        // 没有指定当前身份时返回已有角色列表。
        return orderRolesByCurrentRole(existedRoles, existedRoles.get(0));
    }

    /**
     * 确保用户拥有指定角色。
     */
    private void ensureRole(Long communityId, Long userId, String roleCode) {
        // 先校验角色编码合法性，避免写入脏角色。
        checkRoleCode(roleCode);
        // 查询角色是否已经存在。
        UserRoleEntity existed = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getCommunityId, communityId)
                .eq(UserRoleEntity::getUserId, userId)
                .eq(UserRoleEntity::getRoleCode, roleCode)
                .eq(UserRoleEntity::getDeleted, 0)
                .last("LIMIT 1"));
        // 已存在角色直接返回。
        if (existed != null) {
            // 如果角色被禁用，恢复为正常。
            if (!Objects.equals(existed.getRoleStatus(), STATUS_NORMAL)) {
                // 设置正常状态。
                existed.setRoleStatus(STATUS_NORMAL);
                // 更新角色。
                userRoleMapper.updateById(existed);
            }
            // 结束处理。
            return;
        }
        // 构造新角色。
        UserRoleEntity role = new UserRoleEntity();
        // 设置社区 ID。
        role.setCommunityId(communityId);
        // 设置用户 ID。
        role.setUserId(userId);
        // 设置角色编码。
        role.setRoleCode(roleCode);
        // 设置正常状态。
        role.setRoleStatus(STATUS_NORMAL);
        // 设置未删除。
        role.setDeleted(0);
        // 插入角色。
        userRoleMapper.insert(role);
    }

    /**
     * 查询指定社区内用户。
     */
    private UserAccount selectAccountInCommunity(Long communityId, Long userId) {
        // 使用 community_id 做数据隔离。
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getCommunityId, communityId)
                .eq(UserAccount::getId, userId)
                .eq(UserAccount::getDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 查询用户角色编码。
     */
    private List<String> listRoleCodes(Long communityId, Long userId) {
        // 查询用户在当前社区内的正常角色。
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getCommunityId, communityId)
                        .eq(UserRoleEntity::getUserId, userId)
                        .eq(UserRoleEntity::getRoleStatus, STATUS_NORMAL)
                        .eq(UserRoleEntity::getDeleted, 0))
                .stream()
                .map(UserRoleEntity::getRoleCode)
                .toList();
    }

    /**
     * 按当前角色调整角色列表顺序。
     *
     * @param roles       原角色列表。
     * @param currentRole 当前角色。
     * @return 当前角色排在第一位的新列表。
     */
    private List<String> orderRolesByCurrentRole(List<String> roles, String currentRole) {
        // 当前角色为空或不在角色列表中时保持原顺序。
        if (!StringUtils.hasText(currentRole) || roles == null || !roles.contains(currentRole)) {
            // 返回原列表。
            return roles;
        }
        // 将当前角色放在第一位，其余角色保持原有顺序。
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(currentRole), roles.stream().filter(role -> !role.equals(currentRole))).toList();
    }

    /**
     * 校验角色编码是否合法。
     *
     * @param roleCode 角色编码。
     */
    private void checkRoleCode(String roleCode) {
        // 角色为空或不是系统允许角色时拒绝。
        if (!StringUtils.hasText(roleCode) || !ALLOWED_ROLES.contains(roleCode)) {
            // 抛出参数异常。
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的角色：" + roleCode);
        }
    }

    /**
     * 签发访问令牌和刷新令牌。
     *
     * @param account     用户账号。
     * @param communityId 当前社区 ID。
     * @param roles       当前账号角色。
     * @return 登录响应。
     */
    private LoginVO issueLoginVO(UserAccount account, Long communityId, List<String> roles) {
        // 禁用账号不能签发 Token。
        ensureAccountEnabled(account);
        // 角色列表为空时直接拒绝登录。
        if (roles == null || roles.isEmpty()) {
            // 抛出无权限异常。
            throw new BizException(ErrorCode.FORBIDDEN, "账号没有可用角色");
        }
        // 组装 JWT 登录用户载荷。
        LoginUser loginUser = new LoginUser(account.getId(), communityId, roles, account.getPhone());
        // 生成访问令牌。
        String accessToken = jwtUtil.generateToken(loginUser);
        // 生成随机刷新令牌。
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        // 当前刷新令牌版本为空时按 0 处理。
        Integer refreshVersion = account.getRefreshTokenVersion() == null ? 0 : account.getRefreshTokenVersion();
        // 将 refresh token 写入 Redis，保存用户、社区、当前角色和版本。
        stringRedisTemplate.opsForValue().set(refreshKey(refreshToken), account.getId() + ":" + communityId + ":" + roles.get(0) + ":" + refreshVersion, Duration.ofDays(30));
        // 手机号非空表示已绑定手机号。
        boolean phoneBound = StringUtils.hasText(account.getPhone());
        // 返回登录结果。
        return new LoginVO(account.getId(), loginUser.role(), accessToken, refreshToken, roles, communityId, phoneBound);
    }

    /**
     * 校验账号是否启用。
     *
     * @param account 用户账号。
     */
    private void ensureAccountEnabled(UserAccount account) {
        // 禁用账号不能登录、刷新或切换角色。
        if (account == null || Objects.equals(account.getAccountStatus(), STATUS_DISABLED)) {
            // 抛出禁用异常。
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
    }

    /**
     * 生成 refresh token Redis key。
     *
     * @param refreshToken 刷新令牌。
     * @return Redis key。
     */
    private String refreshKey(String refreshToken) {
        // 使用固定前缀方便后续清理和观测。
        return "auth:refresh:" + refreshToken;
    }

    /**
     * 解析或创建老人账号。
     */
    private UserAccount resolveElderAccount(Long communityId, ElderCreateDTO createDTO) {
        // userId 存在时查询同社区账号。
        if (createDTO.userId() != null) {
            // 查询账号。
            UserAccount account = selectAccountInCommunity(communityId, createDTO.userId());
            // 账号不存在时说明跨社区或 ID 错误。
            if (account == null) {
                // 抛出资源不存在异常。
                throw new BizException(ErrorCode.NOT_FOUND, "老人账号不存在或不属于当前社区");
            }
            // 返回已有账号。
            return account;
        }
        // 没有 userId 时必须传手机号。
        if (!StringUtils.hasText(createDTO.phone())) {
            // 抛出参数错误异常。
            throw new BizException(ErrorCode.PARAM_ERROR, "新增老人资料时 userId 或 phone 必须传一个");
        }
        // 根据手机号查找或创建账号。
        return resolveMockLoginAccount(new MockLoginDTO(null, createDTO.phone(), communityId, List.of(RoleConstants.ELDER)), communityId);
    }

    /**
     * 查询老人档案。
     */
    private ElderProfile selectElderProfile(Long communityId, Long elderUserId) {
        // 使用 community_id 和 elderUserId 查询，防止跨社区访问。
        return elderProfileMapper.selectOne(new LambdaQueryWrapper<ElderProfile>()
                .eq(ElderProfile::getCommunityId, communityId)
                .eq(ElderProfile::getUserId, elderUserId)
                .eq(ElderProfile::getDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 查询绑定关系。
     */
    private FamilyElderBind selectFamilyBind(Long communityId, Long familyUserId, Long elderUserId) {
        // 使用 community_id 隔离绑定关系。
        return familyElderBindMapper.selectOne(new LambdaQueryWrapper<FamilyElderBind>()
                .eq(FamilyElderBind::getCommunityId, communityId)
                .eq(FamilyElderBind::getFamilyUserId, familyUserId)
                .eq(FamilyElderBind::getElderUserId, elderUserId)
                .eq(FamilyElderBind::getBindStatus, BIND_STATUS_BOUND)
                .eq(FamilyElderBind::getDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 转换老人档案 VO。
     */
    private ElderProfileVO toElderProfileVO(ElderProfile elderProfile) {
        // 组装接口返回对象。
        return new ElderProfileVO(
                elderProfile.getId(),
                elderProfile.getUserId(),
                elderProfile.getElderName(),
                elderProfile.getElderPhone(),
                elderProfile.getAge(),
                elderProfile.getAddress(),
                elderProfile.getHealthNote()
        );
    }
}
