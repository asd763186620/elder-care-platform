-- elder-care-platform 第二版小程序端增强数据库迁移脚本。
-- 执行方式：
-- docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 < docs/sql/05-v2-miniapp-enhancement.sql

SET NAMES utf8mb4;

-- =========================================================
-- order_db：订单状态机、Outbox、评价
-- =========================================================
USE order_db;

-- 订单表增加第二版状态时间字段。
ALTER TABLE service_order
    ADD COLUMN service_started_at DATETIME NULL COMMENT '开始服务时间' AFTER assigned_at,
    ADD COLUMN submitted_at DATETIME NULL COMMENT '志愿者提交完成时间' AFTER service_started_at;

-- 第一版 ACCEPTED 状态迁移为第二版 WAIT_SERVICE。
UPDATE service_order
SET order_status = 'WAIT_SERVICE'
WHERE order_status = 'ACCEPTED';

-- 更新订单状态字段注释。
ALTER TABLE service_order
    MODIFY COLUMN order_status VARCHAR(32) NOT NULL COMMENT '订单状态：WAIT_GRAB待抢单，WAIT_SERVICE待服务，IN_SERVICE服务中，WAIT_CONFIRM待确认，COMPLETED已完成，CANCELLED已取消，TIMEOUT_CLOSED超时关闭';

-- 订单 Outbox 本地消息表：保证订单事务和 MQ 消息最终一致。
CREATE TABLE IF NOT EXISTS order_event_outbox (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Outbox主键',
    event_id VARCHAR(64) NOT NULL COMMENT '事件唯一ID',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    exchange_name VARCHAR(128) NOT NULL COMMENT 'RabbitMQ交换机名称',
    routing_key VARCHAR(128) NOT NULL COMMENT 'RabbitMQ路由键',
    payload JSON NOT NULL COMMENT '消息JSON内容',
    status VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '发送状态：INIT待发送，SENT已发送，FAILED发送失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
    last_error VARCHAR(512) DEFAULT NULL COMMENT '最近一次发送失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_event_outbox_event_id (event_id),
    KEY idx_order_event_outbox_status_retry (status, next_retry_time, id),
    KEY idx_order_event_outbox_order (order_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单Outbox本地消息表';

-- 订单评价表：一个订单只能评价一次。
CREATE TABLE IF NOT EXISTS order_evaluation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评价主键',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    elder_user_id BIGINT UNSIGNED NOT NULL COMMENT '老人用户ID',
    family_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '亲情号用户ID，老人本人评价时为空',
    volunteer_user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '社区ID',
    score TINYINT NOT NULL COMMENT '评分，1到5分',
    tags VARCHAR(255) DEFAULT NULL COMMENT '评价标签，逗号分隔',
    content VARCHAR(512) DEFAULT NULL COMMENT '评价内容',
    anonymous TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否匿名：0否，1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_evaluation_order_id (order_id),
    KEY idx_order_evaluation_volunteer (community_id, volunteer_user_id, id),
    KEY idx_order_evaluation_elder (community_id, elder_user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单评价表';

-- =========================================================
-- volunteer_db：志愿者签到
-- =========================================================
USE volunteer_db;

-- 志愿者签到记录表。
CREATE TABLE IF NOT EXISTS volunteer_checkin_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '签到记录主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '社区ID',
    volunteer_user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
    checkin_date DATE NOT NULL COMMENT '签到日期',
    checkin_time DATETIME NOT NULL COMMENT '签到时间',
    longitude DECIMAL(10, 7) NOT NULL COMMENT '签到经度',
    latitude DECIMAL(10, 7) NOT NULL COMMENT '签到纬度',
    address VARCHAR(255) NOT NULL COMMENT '签到地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '签到状态：1正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_volunteer_date (volunteer_user_id, checkin_date),
    KEY idx_volunteer_checkin_community (community_id, volunteer_user_id, id),
    KEY idx_volunteer_checkin_date (community_id, checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿者签到记录表';

-- 志愿者时间补偿记录表：释放时间锁失败时用于后续重试补偿。
CREATE TABLE IF NOT EXISTS volunteer_time_compensation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '补偿记录主键',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    volunteer_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
    start_time DATETIME NOT NULL COMMENT '锁定开始时间',
    end_time DATETIME NOT NULL COMMENT '锁定结束时间',
    compensation_type VARCHAR(32) NOT NULL COMMENT '补偿类型：RELEASE_TIME释放时间锁',
    status VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '补偿状态：INIT待处理，SUCCESS成功，FAILED失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    last_error VARCHAR(512) DEFAULT NULL COMMENT '最近一次失败原因',
    next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_volunteer_time_comp_retry (status, next_retry_time),
    KEY idx_volunteer_time_comp_order (order_id, volunteer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿者时间锁补偿记录表';

-- =========================================================
-- notify_db：消息中心
-- =========================================================
USE notify_db;

-- 消息中心需要已读状态。
ALTER TABLE notify_record
    ADD COLUMN read_status TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态：0未读，1已读' AFTER notify_content;

-- 确保 MQ 消费幂等索引存在。
ALTER TABLE notify_record
    DROP INDEX uk_notify_mq_message,
    ADD UNIQUE KEY uk_notify_mq_message (mq_message_id);

-- 消息列表查询索引。
CREATE INDEX idx_notify_receiver_read ON notify_record (community_id, receiver_user_id, read_status, id);
