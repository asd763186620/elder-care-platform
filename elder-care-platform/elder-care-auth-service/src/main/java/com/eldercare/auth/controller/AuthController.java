package com.eldercare.auth.controller;

import com.eldercare.api.dto.MiniAppLoginDTO;
import com.eldercare.api.dto.PhoneBindDTO;
import com.eldercare.api.dto.RefreshTokenDTO;
import com.eldercare.api.dto.SwitchRoleDTO;
import com.eldercare.api.dto.WechatPhoneBindDTO;
import com.eldercare.api.vo.AuthCurrentVO;
import com.eldercare.api.vo.LoginVO;
import com.eldercare.auth.service.AuthAppService;
import com.eldercare.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证中心接口。
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证中心接口", description = "微信登录、刷新令牌、退出登录、当前用户、手机号绑定和身份切换")
public class AuthController {
    /** 认证业务服务。 */
    private final AuthAppService authAppService;

    public AuthController(AuthAppService authAppService) {
        // 保存认证业务服务。
        this.authAppService = authAppService;
    }

    /** 微信小程序登录。 */
    @PostMapping("/wx-login")
    @Operation(summary = "微信小程序登录", description = "使用 wx.login code 完成注册或静默登录")
    public Result<LoginVO> wxLogin(@Valid @RequestBody MiniAppLoginDTO loginDTO) {
        // 调用认证服务执行登录。
        return Result.success(authAppService.wxLogin(loginDTO));
    }

    /** 刷新 accessToken。 */
    @PostMapping("/refresh-token")
    @Operation(summary = "刷新访问令牌", description = "使用 refreshToken 换取新的 accessToken 和 refreshToken")
    public Result<LoginVO> refreshToken(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO) {
        // 执行刷新令牌。
        return Result.success(authAppService.refreshToken(refreshTokenDTO.refreshToken()));
    }

    /** 退出登录。 */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "删除当前 refreshToken，并递增 refreshVersion")
    public Result<Void> logout(@RequestBody(required = false) RefreshTokenDTO refreshTokenDTO) {
        // 执行退出登录。
        authAppService.logout(refreshTokenDTO == null ? null : refreshTokenDTO.refreshToken());
        // 返回成功。
        return Result.success();
    }

    /** 当前登录用户。 */
    @GetMapping("/current")
    @Operation(summary = "当前登录用户", description = "返回当前账号、角色列表和档案摘要")
    public Result<AuthCurrentVO> current() {
        // 查询当前认证用户。
        return Result.success(authAppService.current());
    }

    /** 绑定手机号。 */
    @PostMapping("/bind-phone")
    @Operation(summary = "绑定手机号", description = "给当前微信账号绑定手机号")
    public Result<Void> bindPhone(@Valid @RequestBody PhoneBindDTO bindDTO) {
        // 绑定手机号。
        authAppService.bindPhone(bindDTO);
        // 返回成功。
        return Result.success();
    }

    /** 绑定微信手机号。 */
    @PostMapping("/bind-wx-phone")
    @Operation(summary = "绑定微信手机号", description = "使用小程序 getPhoneNumber code 换取可信手机号并绑定")
    public Result<Void> bindWechatPhone(@Valid @RequestBody WechatPhoneBindDTO bindDTO) {
        // 绑定微信手机号。
        authAppService.bindWechatPhone(bindDTO);
        // 返回成功。
        return Result.success();
    }

    /** 切换当前身份。 */
    @PostMapping("/switch-role")
    @Operation(summary = "切换当前身份", description = "多身份账号切换当前角色并重新签发 Token")
    public Result<LoginVO> switchRole(@Valid @RequestBody SwitchRoleDTO roleDTO) {
        // 切换身份。
        return Result.success(authAppService.switchRole(roleDTO));
    }
}
