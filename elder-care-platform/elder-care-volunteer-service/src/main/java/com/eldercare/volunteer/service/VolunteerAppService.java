package com.eldercare.volunteer.service;

import com.eldercare.api.dto.*;
import com.eldercare.api.vo.VolunteerBriefVO;

import java.util.List;

/**
 * 志愿者业务接口。
 */
public interface VolunteerAppService {
    /**
     * 保存或更新当前登录志愿者的资料。
     *
     * @param dto 志愿者资料请求。
     */
    void saveProfile(VolunteerProfileDTO dto);

    /**
     * 新增当前登录志愿者的可服务时间。
     *
     * @param dto 可服务时间请求。
     */
    void addAvailableTime(VolunteerAvailableTimeDTO dto);

    /**
     * 查询指定服务项目和时间段内可用的志愿者。
     *
     * @param dto 查询条件。
     * @return 可用志愿者列表。
     */
    List<VolunteerBriefVO> listAvailable(AvailableVolunteerQueryDTO dto);

    /**
     * 内部接口使用：校验志愿者在某个时间段是否可用。
     *
     * @param dto 校验条件。
     * @return true 表示可用。
     */
    boolean checkAvailable(VolunteerCheckAvailableDTO dto);

    /**
     * 内部接口使用：锁定志愿者服务时间，防止并发接单冲突。
     *
     * @param dto 锁定请求。
     * @return true 表示锁定成功。
     */
    boolean lockTime(VolunteerLockTimeDTO dto);

    /**
     * 内部接口使用：释放志愿者服务时间。
     *
     * @param dto 释放请求。
     * @return true 表示释放成功。
     */
    boolean releaseTime(VolunteerLockTimeDTO dto);
}
