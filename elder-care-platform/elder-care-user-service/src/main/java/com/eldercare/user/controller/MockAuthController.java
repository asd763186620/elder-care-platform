package com.eldercare.user.controller;

import com.eldercare.api.dto.*;
import com.eldercare.api.vo.LoginVO;
import com.eldercare.common.response.Result;
import com.eldercare.user.service.UserAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟登录控制器。
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证接口", description = "小程序登录、刷新令牌、退出登录、绑定手机号和角色切换")
public class MockAuthController {

    /**
     * 用户业务服务。
     */
    private final UserAppService userAppService;

    /**
     * 构造控制器。
     *
     * @param userAppService 用户业务服务。
     */
    public MockAuthController(UserAppService userAppService) {
        // 保存用户业务服务。
        this.userAppService = userAppService;
    }

    /**
     * 第一版模拟登录接口。
     *
     * @param loginDTO 模拟登录请求。
     * @return JWT 登录结果。
     */
    @PostMapping("/mock-login")
    @Operation(summary = "模拟登录", description = "本地调试使用，按 userId 或手机号生成登录态")
    public Result<LoginVO> mockLogin(@Valid @RequestBody MockLoginDTO loginDTO) {
        // 调用业务服务生成 Token。
        return Result.success(userAppService.mockLogin(loginDTO));
    }

    /**
     * 微信小程序真实登录入口。
     *
     * @param loginDTO 小程序登录请求。
     * @return JWT 登录结果。
     */
    @PostMapping("/wx-login")
    @Operation(summary = "微信小程序登录", description = "使用 wx.login 返回的 code 调用 code2session，并完成注册或静默登录")
    public Result<LoginVO> wxLogin(@Valid @RequestBody MiniAppLoginDTO loginDTO) {
        // 调用用户服务执行 code2session、注册或静默登录。
        return Result.success(userAppService.miniAppLogin(loginDTO));
    }

    /**
     * 使用刷新令牌换取新访问令牌。
     *
     * @param refreshTokenDTO 刷新请求。
     * @return 新 JWT 登录结果。
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "刷新访问令牌", description = "使用 refreshToken 换取新的 access token 和 refreshToken")
    public Result<LoginVO> refreshToken(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO) {
        // 刷新令牌不走 Authorization 请求头，直接用请求体中的 refreshToken。
        return Result.success(userAppService.refreshToken(refreshTokenDTO.refreshToken()));
    }

    /**
     * 当前用户退出登录。
     *
     * @return 空结果。
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "递增刷新令牌版本，使历史 refreshToken 失效")
    public Result<Void> logout() {
        // 退出时递增账号刷新令牌版本并清理当前用户 refresh token。
        userAppService.logout();
        // 返回成功。
        return Result.success();
    }

    /**
     * 当前用户绑定手机号。
     *
     * @param bindDTO 绑定手机号请求。
     * @return 空结果。
     */
    @PostMapping("/bind-phone")
    @Operation(summary = "绑定手机号", description = "给当前微信账号绑定手机号")
    public Result<Void> bindPhone(@Valid @RequestBody PhoneBindDTO bindDTO) {
        // 绑定手机号后后续可按手机号找回或运营联系。
        userAppService.bindPhone(bindDTO);
        // 返回成功。
        return Result.success();
    }

    /**
     * 使用微信手机号凭证绑定手机号。
     *
     * @param bindDTO 微信手机号绑定请求。
     * @return 空结果。
     */
    @PostMapping("/bind-wx-phone")
    @Operation(summary = "绑定微信手机号", description = "使用小程序 getPhoneNumber 返回的 code 换取可信手机号并绑定")
    public Result<Void> bindWechatPhone(@Valid @RequestBody WechatPhoneBindDTO bindDTO) {
        // 调用用户服务完成微信手机号解析和绑定。
        userAppService.bindWechatPhone(bindDTO);
        // 返回成功。
        return Result.success();
    }

    /**
     * 多身份账号切换当前角色。
     *
     * @param roleDTO 角色切换请求。
     * @return 新 JWT 登录结果。
     */
    @PostMapping("/switch-role")
    @Operation(summary = "切换当前角色", description = "多身份账号切换当前启用角色并重新签发 Token")
    public Result<LoginVO> switchRole(@Valid @RequestBody SwitchRoleDTO roleDTO) {
        // 切换角色后重新签发 Token，网关后续透传新的角色信息。
        return Result.success(userAppService.switchRole(roleDTO));
    }
}
