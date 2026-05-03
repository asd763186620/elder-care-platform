package com.eldercare.user.service;

import com.eldercare.api.dto.ElderCreateDTO;
import com.eldercare.api.dto.FamilyBindDTO;
import com.eldercare.api.dto.MockLoginDTO;
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
