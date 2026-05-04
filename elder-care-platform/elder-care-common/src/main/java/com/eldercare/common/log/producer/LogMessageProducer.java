package com.eldercare.common.log.producer;

import com.eldercare.common.constant.MqConstants;
import com.eldercare.common.log.dto.ApiAccessLogMessage;
import com.eldercare.common.log.dto.OperationLogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * 日志消息生产者。
 * 说明：发送失败只记录 error，不向外抛异常，避免日志功能影响主业务。
 */
@Component
@ConditionalOnClass(RabbitTemplate.class)
public class LogMessageProducer {
    /** 日志对象。 */
    private static final Logger log = LoggerFactory.getLogger(LogMessageProducer.class);
    /** RabbitMQ 发送模板 Provider，避免 Bean 创建顺序导致 Producer 不注册。 */
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    public LogMessageProducer(ObjectProvider<RabbitTemplate> rabbitTemplateProvider) {
        // 保存 RabbitTemplate Provider，发送时再获取真实模板。
        this.rabbitTemplateProvider = rabbitTemplateProvider;
    }

    /**
     * 发送接口访问日志。
     */
    public void sendApiAccessLog(ApiAccessLogMessage message) {
        try {
            // 获取 RabbitTemplate；没有 Rabbit 配置时直接跳过，不能影响主流程。
            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            // RabbitTemplate 不存在说明当前服务没有启用 RabbitMQ。
            if (rabbitTemplate == null) {
                // 打印调试日志，避免业务服务因为日志能力启动失败。
                log.debug("skip api access log because RabbitTemplate is not available, traceId={}", message == null ? null : message.traceId());
                // 结束发送。
                return;
            }
            // 发送到日志交换机。
            rabbitTemplate.convertAndSend(MqConstants.LOG_EXCHANGE, MqConstants.API_ACCESS_LOG_ROUTING_KEY, message);
        } catch (Exception exception) {
            // 日志发送失败不能影响主流程。
            log.error("send api access log failed, traceId={}", message == null ? null : message.traceId(), exception);
        }
    }

    /**
     * 发送操作审计日志。
     */
    public void sendOperationLog(OperationLogMessage message) {
        try {
            // 获取 RabbitTemplate；没有 Rabbit 配置时直接跳过，不能影响主流程。
            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            // RabbitTemplate 不存在说明当前服务没有启用 RabbitMQ。
            if (rabbitTemplate == null) {
                // 打印调试日志，避免业务服务因为日志能力启动失败。
                log.debug("skip operation log because RabbitTemplate is not available, traceId={}", message == null ? null : message.traceId());
                // 结束发送。
                return;
            }
            // 发送到日志交换机。
            rabbitTemplate.convertAndSend(MqConstants.LOG_EXCHANGE, MqConstants.OPERATION_LOG_ROUTING_KEY, message);
        } catch (Exception exception) {
            // 日志发送失败不能影响主流程。
            log.error("send operation log failed, traceId={}", message == null ? null : message.traceId(), exception);
        }
    }
}
