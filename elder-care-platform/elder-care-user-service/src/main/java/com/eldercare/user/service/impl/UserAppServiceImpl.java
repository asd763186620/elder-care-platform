package com.eldercare.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.api.dto.ElderCreateDTO;
import com.eldercare.api.dto.FamilyBindDTO;
import com.eldercare.api.dto.MockLoginDTO;
import com.eldercare.api.vo.ElderProfileVO;
import com.eldercare.api.vo.LoginVO;
import com.eldercare.api.vo.UserInfoVO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
     * 已绑定状态。
     */
    private static final int BIND_STATUS_BOUND = 2;

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
     * 构造业务服务。
     */
    public UserAppServiceImpl(UserAccountMapper userAccountMapper,
                              UserRoleMapper userRoleMapper,
                              ElderProfileMapper elderProfileMapper,
                              FamilyElderBindMapper familyElderBindMapper,
                              JwtUtil jwtUtil) {
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
        // 解析登录角色，不传时默认老人。
        List<String> roles = resolveRoles(loginDTO.roles());
        // 确保账号拥有这些角色。
        roles.forEach(role -> ensureRole(communityId, account.getId(), role));
        // 更新最近登录时间。
        account.setLastLoginTime(LocalDateTime.now());
        // 持久化最近登录时间。
        userAccountMapper.updateById(account);
        // 生成 JWT 登录用户载荷。
        LoginUser loginUser = new LoginUser(account.getId(), communityId, roles, account.getPhone());
        // 生成 JWT。
        String token = jwtUtil.generateToken(loginUser);
        // 返回登录结果，主角色使用第一个角色。
        return new LoginVO(account.getId(), loginUser.role(), token);
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
        List<String> cleaned = roles.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
        // 清理后为空也默认老人。
        return cleaned.isEmpty() ? List.of(RoleConstants.ELDER) : cleaned;
    }

    /**
     * 确保用户拥有指定角色。
     */
    private void ensureRole(Long communityId, Long userId, String roleCode) {
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
