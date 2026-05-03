package com.eldercare.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.user.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户账号 Mapper。
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}
