package com.eldercare.log.consumer;

import com.eldercare.common.constant.MqConstants;
import com.eldercare.common.log.dto.ApiAccessLogMessage;
import com.eldercare.log.entity.ApiAccessLog;
import com.eldercare.log.mapper.ApiAccessLogMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 接口访问日志消费者。
 */
@Component
public class ApiAccessLogConsumer {
    /** 接口访问日志 Mapper。 */
    private final ApiAccessLogMapper mapper;

    public ApiAccessLogConsumer(ApiAccessLogMapper mapper) {
        // 保存 Mapper。
        this.mapper = mapper;
    }

    /**
     * 消费接口访问日志消息。
     */
    @RabbitListener(queues = MqConstants.API_ACCESS_LOG_QUEUE)
    public void consume(ApiAccessLogMessage message) {
        // 将消息写入日志表。
        mapper.insert(ApiAccessLog.from(message));
    }
}
