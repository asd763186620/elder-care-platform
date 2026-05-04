package com.eldercare.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.auth.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户账号 Mapper。
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}
