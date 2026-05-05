package com.eldercare.log.consumer;

import com.eldercare.common.enums.MqEnum;
import com.eldercare.common.log.dto.OperationLogMessage;
import com.eldercare.log.entity.OperationLog;
import com.eldercare.log.mapper.OperationLogMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 操作日志消费者。
 */
@Component
public class OperationLogConsumer {
    /** 操作日志 Mapper。 */
    private final OperationLogMapper mapper;

    public OperationLogConsumer(OperationLogMapper mapper) {
        // 保存 Mapper。
        this.mapper = mapper;
    }

    /**
     * 消费操作日志消息。
     */
    @RabbitListener(queues = "#{T(com.eldercare.common.enums.MqEnum).OPERATION_LOG_QUEUE.code()}")
    public void consume(OperationLogMessage message) {
        // 将消息写入日志表。
        mapper.insert(OperationLog.from(message));
    }
}
