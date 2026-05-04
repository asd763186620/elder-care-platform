package com.eldercare.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.auth.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色 Mapper。
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {
}
