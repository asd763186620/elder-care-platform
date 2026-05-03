package com.eldercare.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.order.entity.ServiceOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServiceOrderMapper extends BaseMapper<ServiceOrder> {
}
