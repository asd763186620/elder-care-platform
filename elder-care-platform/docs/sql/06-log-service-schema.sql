-- 日志服务数据库初始化脚本。
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS elder_log DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE elder_log;

-- 接口访问日志表。
CREATE TABLE IF NOT EXISTS api_access_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL COMMENT '链路ID',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID',
    role_type VARCHAR(32) DEFAULT NULL COMMENT '当前身份',
    community_id BIGINT UNSIGNED DEFAULT NULL COMMENT '社区ID',
    request_method VARCHAR(16) NOT NULL COMMENT '请求方法',
    request_uri VARCHAR(255) NOT NULL COMMENT '请求路径',
    query_params TEXT DEFAULT NULL COMMENT '请求参数',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT '客户端User-Agent',
    status_code INT DEFAULT NULL COMMENT '响应状态码',
    cost_time BIGINT NOT NULL COMMENT '耗时，毫秒',
    slow_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否慢接口：0否，1是',
    error_message TEXT DEFAULT NULL COMMENT '异常信息',
    request_time DATETIME NOT NULL COMMENT '请求时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_trace_id (trace_id),
    KEY idx_user_id (user_id),
    KEY idx_community_id (community_id),
    KEY idx_request_uri (request_uri),
    KEY idx_cost_time (cost_time),
    KEY idx_slow_flag (slow_flag),
    KEY idx_request_time (request_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口访问日志表';

-- 操作审计日志表。
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL COMMENT '链路ID',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID',
    role_type VARCHAR(32) DEFAULT NULL COMMENT '当前身份',
    community_id BIGINT UNSIGNED DEFAULT NULL COMMENT '社区ID',
    module VARCHAR(64) NOT NULL COMMENT '业务模块',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    description VARCHAR(255) DEFAULT NULL COMMENT '操作描述',
    biz_id VARCHAR(128) DEFAULT NULL COMMENT '业务ID',
    class_name VARCHAR(255) NOT NULL COMMENT '类名',
    method_name VARCHAR(128) NOT NULL COMMENT '方法名',
    request_params TEXT DEFAULT NULL COMMENT '请求参数JSON',
    response_result TEXT DEFAULT NULL COMMENT '响应结果JSON',
    success_flag TINYINT NOT NULL COMMENT '是否成功：0否，1是',
    error_message TEXT DEFAULT NULL COMMENT '异常信息',
    cost_time BIGINT NOT NULL COMMENT '耗时，毫秒',
    operation_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_trace_id (trace_id),
    KEY idx_user_id (user_id),
    KEY idx_community_id (community_id),
    KEY idx_module (module),
    KEY idx_operation_type (operation_type),
    KEY idx_biz_id (biz_id),
    KEY idx_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';
