-- elder-care-platform 生产级补强迁移脚本。
-- 说明：本脚本不删除旧状态，兼容历史 WAIT_GRAB / WAIT_SERVICE 数据。

SET NAMES utf8mb4;

-- =========================================================
-- order_db：生产级状态语义和 CAS 抢单索引
-- =========================================================
USE order_db;

-- 将公共池待抢单迁移为生产语义：PENDING_ASSIGN。
UPDATE service_order
SET order_status = 'PENDING_ASSIGN'
WHERE order_status = 'WAIT_GRAB';

-- 将已接单/待服务迁移为生产语义：ASSIGNED。
UPDATE service_order
SET order_status = 'ASSIGNED'
WHERE order_status = 'WAIT_SERVICE';

-- 更新订单状态字段注释。
ALTER TABLE service_order
    MODIFY COLUMN order_status VARCHAR(32) NOT NULL COMMENT '订单状态：PENDING_ASSIGN待分配，ASSIGNED已分配，IN_SERVICE服务中，WAIT_CONFIRM待确认，COMPLETED已完成，CANCELLED已取消，TIMEOUT_CLOSED超时关闭；兼容历史WAIT_GRAB/WAIT_SERVICE';

-- 抢单 CAS 查询/更新使用 community_id + id + status + volunteer + version，补充复合索引降低锁等待。
CREATE INDEX idx_service_order_grab_cas
    ON service_order (community_id, id, order_status, assigned_volunteer_user_id, version);

-- order_event_outbox 是订单服务本地消息表，等价于 local_message 语义。
-- 为了排查时更容易按状态和时间扫描，确保重试索引存在。
CREATE INDEX idx_order_event_outbox_retry
    ON order_event_outbox (status, next_retry_time, id);

-- =========================================================
-- notify_db：MQ 消费幂等索引
-- =========================================================
USE notify_db;

-- notify_record.mq_message_id 必须唯一，防止 RabbitMQ 重复投递导致重复通知。
-- 01-init-schema.sql 已包含该唯一索引；这里保留显式说明，老库如缺失请手工执行下方语句。
-- ALTER TABLE notify_record ADD UNIQUE KEY uk_notify_mq_message (mq_message_id);
