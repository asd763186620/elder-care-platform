package com.eldercare.user.service;

import com.eldercare.api.dto.*;
import com.eldercare.api.vo.AuthCurrentVO;
import com.eldercare.api.vo.ElderProfileVO;
import com.eldercare.api.vo.LoginVO;
import com.eldercare.api.vo.UserInfoVO;

import java.util.List;

/**
 * 用户服务业务接口。
 */
public interface UserAppService {

    /**
     * 模拟登录。
     *
     * @param loginDTO 登录请求。
     * @return 登录结果。
     */
    LoginVO mockLogin(MockLoginDTO loginDTO);

    /**
     * 微信小程序登录。
     *
     * @param loginDTO 小程序登录请求。
     * @return 登录结果。
     */
    LoginVO miniAppLogin(MiniAppLoginDTO loginDTO);

    /**
     * 使用刷新令牌换取新的访问令牌。
     *
     * @param refreshToken 刷新令牌。
     * @return 新登录结果。
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * 当前用户退出登录。
     */
    void logout();

    /**
     * 当前用户退出登录，并删除指定刷新令牌。
     *
     * @param refreshToken 当前客户端保存的 refreshToken。
     */
    void logout(String refreshToken);

    /**
     * 绑定当前账号手机号。
     *
     * @param bindDTO 绑定手机号请求。
     */
    void bindPhone(PhoneBindDTO bindDTO);

    /**
     * 使用微信手机号凭证绑定手机号。
     *
     * @param bindDTO 微信手机号绑定请求。
     */
    void bindWechatPhone(WechatPhoneBindDTO bindDTO);

    /**
     * 切换当前账号启用角色。
     *
     * @param roleDTO 角色切换请求。
     * @return 新登录结果。
     */
    LoginVO switchRole(SwitchRoleDTO roleDTO);

    /**
     * 查询认证维度的当前登录用户信息。
     *
     * @return 当前登录用户、角色和档案摘要。
     */
    AuthCurrentVO currentAuth();

    /**
     * 查询当前登录用户。
     *
     * @return 当前用户信息。
     */
    UserInfoVO getCurrentUser();

    /**
     * 新增老人资料。
     *
     * @param createDTO 新增老人请求。
     * @return 老人资料。
     */
    ElderProfileVO createElder(ElderCreateDTO createDTO);

    /**
     * 亲情号绑定老人。
     *
     * @param bindDTO 绑定请求。
     */
    void bindElder(FamilyBindDTO bindDTO);

    /**
     * 查询当前亲情号绑定的老人列表。
     *
     * @return 老人列表。
     */
    List<ElderProfileVO> listBoundElders();

    /**
     * 校验亲情号是否绑定老人。
     *
     * @param communityId  社区 ID。
     * @param familyUserId 亲情号用户 ID。
     * @param elderUserId  老人用户 ID。
     * @return true 表示可操作。
     */
    boolean checkFamilyBind(Long communityId, Long familyUserId, Long elderUserId);

    /**
     * 判断用户是否存在。
     *
     * @param userId 用户 ID。
     * @return true 表示存在。
     */
    boolean existsById(Long userId);
}
