package com.eldercare.auth.service;

import com.eldercare.api.dto.MiniAppLoginDTO;
import com.eldercare.api.dto.PhoneBindDTO;
import com.eldercare.api.dto.SwitchRoleDTO;
import com.eldercare.api.dto.WechatPhoneBindDTO;
import com.eldercare.api.vo.AuthCurrentVO;
import com.eldercare.api.vo.LoginVO;

/**
 * 认证服务业务接口。
 */
public interface AuthAppService {
    /** 微信小程序登录。 */
    LoginVO wxLogin(MiniAppLoginDTO loginDTO);

    /** 刷新访问令牌。 */
    LoginVO refreshToken(String refreshToken);

    /** 退出登录。 */
    void logout(String refreshToken);

    /** 绑定手机号。 */
    void bindPhone(PhoneBindDTO bindDTO);

    /** 绑定微信手机号。 */
    void bindWechatPhone(WechatPhoneBindDTO bindDTO);

    /** 切换当前身份。 */
    LoginVO switchRole(SwitchRoleDTO roleDTO);

    /** 查询当前登录用户。 */
    AuthCurrentVO current();
}
