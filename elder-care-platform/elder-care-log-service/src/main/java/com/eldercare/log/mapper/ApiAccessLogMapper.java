package com.eldercare.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.log.entity.ApiAccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 接口访问日志 Mapper。
 */
@Mapper
public interface ApiAccessLogMapper extends BaseMapper<ApiAccessLog> {
}
