package com.eldercare.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.order.entity.OrderEventOutbox;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Outbox Mapper。
 */
@Mapper
public interface OrderEventOutboxMapper extends BaseMapper<OrderEventOutbox> {
}
