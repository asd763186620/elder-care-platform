-- 社区养老服务预约小程序第一版数据库初始化脚本。
-- MySQL 版本要求：MySQL 8.x。
-- 设计原则：按微服务拆库，每个核心业务表都保留 community_id，方便社区级数据隔离。

-- 强制当前 SQL 会话使用 utf8mb4，避免 mysql 客户端默认 latin1 导致中文注释或数据乱码。
SET NAMES utf8mb4;

-- 创建用户服务数据库。
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建社区服务数据库。
CREATE DATABASE IF NOT EXISTS community_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建志愿者服务数据库。
CREATE DATABASE IF NOT EXISTS volunteer_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建订单服务数据库。
CREATE DATABASE IF NOT EXISTS order_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建通知服务数据库。
CREATE DATABASE IF NOT EXISTS notify_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =========================================================
-- user_db：用户、角色、老人档案、亲情号绑定
-- =========================================================
USE user_db;

-- 用户账号表：一个手机号对应一个账号，账号可以拥有多个身份。
CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户账号主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID，社区级平台的数据隔离字段',
    phone VARCHAR(20) DEFAULT NULL COMMENT '登录手机号，微信首次静默登录时可为空，绑定手机号后写入',
    open_id VARCHAR(128) DEFAULT NULL COMMENT '微信小程序openId，同一小程序内唯一',
    union_id VARCHAR(128) DEFAULT NULL COMMENT '微信开放平台unionId，未绑定开放平台时可为空',
    password_hash VARCHAR(255) DEFAULT NULL COMMENT '密码哈希，小程序验证码登录时可以为空',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    real_name VARCHAR(64) DEFAULT NULL COMMENT '实名姓名，第一版可为空',
    id_card_no VARCHAR(32) DEFAULT NULL COMMENT '身份证号，建议业务层加密或脱敏存储',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0未知，1男，2女',
    account_status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1正常，2禁用',
    current_role VARCHAR(32) DEFAULT NULL COMMENT '当前启用角色，多身份账号切换时更新',
    refresh_token_version INT NOT NULL DEFAULT 0 COMMENT '刷新令牌版本，退出登录或强制下线时递增',
    last_login_time DATETIME DEFAULT NULL COMMENT '最近登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_account_phone (phone),
    UNIQUE KEY uk_user_account_open_id (open_id),
    KEY idx_user_account_union_id (union_id),
    KEY idx_user_account_community (community_id),
    KEY idx_user_account_status (account_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号表';

-- 用户角色表：支持一个账号同时是老人、亲情号、志愿者。
CREATE TABLE IF NOT EXISTS user_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户角色主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户账号ID',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码：ELDER，FAMILY，VOLUNTEER',
    role_status TINYINT NOT NULL DEFAULT 1 COMMENT '角色状态：1正常，2禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role_user_role_community (community_id, user_id, role_code),
    KEY idx_user_role_user (user_id),
    KEY idx_user_role_role (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色表';

-- 老人档案表：老人身份的扩展资料。
CREATE TABLE IF NOT EXISTS elder_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '老人档案主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '老人对应的用户账号ID',
    elder_name VARCHAR(64) NOT NULL COMMENT '老人姓名',
    elder_phone VARCHAR(20) DEFAULT NULL COMMENT '老人联系电话',
    age INT DEFAULT NULL COMMENT '年龄',
    address VARCHAR(255) DEFAULT NULL COMMENT '详细住址',
    health_note VARCHAR(512) DEFAULT NULL COMMENT '健康情况备注，例如慢病、行动不便等',
    emergency_contact_name VARCHAR(64) DEFAULT NULL COMMENT '紧急联系人姓名',
    emergency_contact_phone VARCHAR(20) DEFAULT NULL COMMENT '紧急联系人手机号',
    profile_status TINYINT NOT NULL DEFAULT 1 COMMENT '档案状态：1正常，2停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_elder_profile_user_community (community_id, user_id),
    KEY idx_elder_profile_phone (elder_phone),
    KEY idx_elder_profile_status (profile_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老人档案表';

-- 亲情号绑定老人表：支持亲属代老人下单。
CREATE TABLE IF NOT EXISTS family_elder_bind (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '亲情号绑定主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    family_user_id BIGINT UNSIGNED NOT NULL COMMENT '亲情号用户ID',
    elder_user_id BIGINT UNSIGNED NOT NULL COMMENT '老人用户ID',
    relationship VARCHAR(32) NOT NULL COMMENT '关系：CHILD子女，SPOUSE配偶，RELATIVE亲属，OTHER其他',
    bind_status TINYINT NOT NULL DEFAULT 1 COMMENT '绑定状态：1待确认，2已绑定，3已拒绝，4已解绑',
    bind_source VARCHAR(32) NOT NULL DEFAULT 'MINI_APP' COMMENT '绑定来源：MINI_APP小程序，STAFF后台',
    confirmed_at DATETIME DEFAULT NULL COMMENT '老人确认绑定时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_family_elder_bind_pair (community_id, family_user_id, elder_user_id),
    KEY idx_family_elder_bind_family (community_id, family_user_id, bind_status),
    KEY idx_family_elder_bind_elder (community_id, elder_user_id, bind_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='亲情号绑定老人表';

-- =========================================================
-- community_db：社区、服务项目
-- =========================================================
USE community_db;

-- 社区表：社区级平台的组织边界。
CREATE TABLE IF NOT EXISTS community (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '社区主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '社区ID，和主键保持一致或由业务生成',
    community_name VARCHAR(128) NOT NULL COMMENT '社区名称',
    province VARCHAR(64) DEFAULT NULL COMMENT '省份',
    city VARCHAR(64) DEFAULT NULL COMMENT '城市',
    district VARCHAR(64) DEFAULT NULL COMMENT '区县',
    address VARCHAR(255) DEFAULT NULL COMMENT '社区详细地址',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '社区联系人',
    contact_phone VARCHAR(20) DEFAULT NULL COMMENT '社区联系电话',
    community_status TINYINT NOT NULL DEFAULT 1 COMMENT '社区状态：1正常，2停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_community_id (community_id),
    KEY idx_community_status (community_status),
    KEY idx_community_region (province, city, district)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区表';

-- 服务项目表：例如陪诊、上门理发、助餐、家政等。
CREATE TABLE IF NOT EXISTS service_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '服务项目主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    item_name VARCHAR(128) NOT NULL COMMENT '服务项目名称',
    item_code VARCHAR(64) NOT NULL COMMENT '服务项目编码',
    item_desc VARCHAR(512) DEFAULT NULL COMMENT '服务项目说明',
    duration_minutes INT NOT NULL DEFAULT 60 COMMENT '默认服务时长，单位分钟',
    price_cent INT NOT NULL DEFAULT 0 COMMENT '服务价格，单位分，公益服务可为0',
    item_status TINYINT NOT NULL DEFAULT 1 COMMENT '项目状态：1启用，2停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_item_code (community_id, item_code),
    KEY idx_service_item_status (community_id, item_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务项目表';

-- =========================================================
-- volunteer_db：志愿者资料、可服务时间、时间锁
-- =========================================================
USE volunteer_db;

-- 志愿者档案表：志愿者身份的扩展资料。
CREATE TABLE IF NOT EXISTS volunteer_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '志愿者档案主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者对应的用户账号ID',
    volunteer_name VARCHAR(64) NOT NULL COMMENT '志愿者姓名',
    volunteer_phone VARCHAR(20) DEFAULT NULL COMMENT '志愿者联系电话',
    skill_tags VARCHAR(255) DEFAULT NULL COMMENT '技能标签，逗号分隔，例如陪诊,助餐,家政',
    service_radius_meter INT DEFAULT NULL COMMENT '可服务半径，单位米',
    profile_status TINYINT NOT NULL DEFAULT 1 COMMENT '档案状态：1待审核，2正常，3停用',
    audit_time DATETIME DEFAULT NULL COMMENT '审核通过时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_volunteer_profile_user_community (community_id, user_id),
    KEY idx_volunteer_profile_status (community_id, profile_status),
    KEY idx_volunteer_profile_phone (volunteer_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿者档案表';

-- 志愿者可服务时间表：志愿者提前设置自己哪些时间可以服务。
CREATE TABLE IF NOT EXISTS volunteer_available_time (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '可服务时间主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    volunteer_user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
    service_item_id BIGINT UNSIGNED DEFAULT NULL COMMENT '可服务项目ID，NULL表示不限服务项目',
    available_date DATE NOT NULL COMMENT '可服务日期',
    start_time DATETIME NOT NULL COMMENT '可服务开始时间',
    end_time DATETIME NOT NULL COMMENT '可服务结束时间',
    available_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1可用，2已锁定，3已取消，4已过期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_available_time_query (community_id, service_item_id, available_status, start_time, end_time),
    KEY idx_available_time_volunteer (community_id, volunteer_user_id, available_date, available_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿者可服务时间表';

-- 志愿者时间锁表：下单指定志愿者或抢单成功时写入，防止同一志愿者同一时间段被重复占用。
CREATE TABLE IF NOT EXISTS volunteer_time_lock (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '志愿者时间锁主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    volunteer_user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '占用该时间段的预约单ID',
    lock_date DATE NOT NULL COMMENT '锁定日期',
    start_time DATETIME NOT NULL COMMENT '锁定开始时间',
    end_time DATETIME NOT NULL COMMENT '锁定结束时间',
    time_slot_key VARCHAR(64) NOT NULL COMMENT '固定时间槽标识，例如202605041000_202605041100',
    lock_status TINYINT NOT NULL DEFAULT 1 COMMENT '锁状态：1已锁定，2已释放',
    active_time_slot_key VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN lock_status = 1 AND deleted = 0 THEN time_slot_key ELSE NULL END) STORED COMMENT '仅活跃锁参与唯一约束的时间槽',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_volunteer_time_slot_active (community_id, volunteer_user_id, active_time_slot_key),
    UNIQUE KEY uk_volunteer_time_lock_order (community_id, order_id),
    KEY idx_volunteer_time_lock_query (community_id, volunteer_user_id, lock_date, lock_status),
    KEY idx_volunteer_time_lock_range (community_id, volunteer_user_id, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿者时间锁表';

-- 注意：uk_volunteer_time_slot_active 依赖业务使用固定时间槽。
-- 如果后续允许任意起止时间，业务层还必须在事务内执行区间重叠检查：
-- start_time < 新end_time AND end_time > 新start_time AND lock_status = 1。

-- =========================================================
-- order_db：预约单、状态流转、抢单记录
-- =========================================================
USE order_db;

-- 预约单表：支持指定志愿者和公共订单池。
CREATE TABLE IF NOT EXISTS service_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预约单主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    order_no VARCHAR(64) NOT NULL COMMENT '预约单号，全局唯一',
    elder_user_id BIGINT UNSIGNED NOT NULL COMMENT '被服务老人用户ID',
    creator_user_id BIGINT UNSIGNED NOT NULL COMMENT '下单人用户ID，老人本人或亲情号',
    creator_role VARCHAR(32) NOT NULL COMMENT '下单人角色：ELDER，FAMILY',
    service_item_id BIGINT UNSIGNED NOT NULL COMMENT '服务项目ID',
    service_address VARCHAR(255) NOT NULL COMMENT '服务地址',
    service_start_time DATETIME NOT NULL COMMENT '预约服务开始时间',
    service_end_time DATETIME NOT NULL COMMENT '预约服务结束时间',
    assign_mode VARCHAR(32) NOT NULL COMMENT '分配方式：DIRECT指定志愿者，PUBLIC_POOL公共订单池',
    specified_volunteer_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '指定的志愿者用户ID，仅DIRECT时有值',
    assigned_volunteer_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '最终接单志愿者用户ID',
    order_status VARCHAR(32) NOT NULL COMMENT '订单状态：CREATED待处理，WAITING_GRAB待抢单，ASSIGNED已分配，IN_SERVICE服务中，COMPLETED已完成，CANCELLED已取消',
    order_source VARCHAR(32) NOT NULL DEFAULT 'MINI_APP' COMMENT '订单来源：MINI_APP小程序，STAFF后台',
    remark VARCHAR(512) DEFAULT NULL COMMENT '预约备注',
    cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    grab_deadline DATETIME DEFAULT NULL COMMENT '公共池抢单截止时间',
    assigned_at DATETIME DEFAULT NULL COMMENT '分配或抢单成功时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    cancelled_at DATETIME DEFAULT NULL COMMENT '取消时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，用于并发更新订单',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_order_no (order_no),
    KEY idx_service_order_elder (community_id, elder_user_id, created_at),
    KEY idx_service_order_creator (community_id, creator_user_id, created_at),
    KEY idx_service_order_volunteer (community_id, assigned_volunteer_user_id, service_start_time),
    KEY idx_service_order_pool (community_id, assign_mode, order_status, service_start_time),
    KEY idx_service_order_status (community_id, order_status, created_at),
    KEY idx_service_order_time (community_id, service_start_time, service_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约单表';

-- 订单状态日志表：记录订单状态流转，便于追踪和审计。
CREATE TABLE IF NOT EXISTS order_status_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单状态日志主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '预约单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '预约单号',
    from_status VARCHAR(32) DEFAULT NULL COMMENT '流转前状态，创建订单时可为空',
    to_status VARCHAR(32) NOT NULL COMMENT '流转后状态',
    operator_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人用户ID，系统操作可为空',
    operator_role VARCHAR(32) DEFAULT NULL COMMENT '操作人角色',
    operate_type VARCHAR(32) NOT NULL COMMENT '操作类型：CREATE，ASSIGN，GRAB，START，COMPLETE，CANCEL',
    operate_remark VARCHAR(255) DEFAULT NULL COMMENT '操作备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order_status_log_order (community_id, order_id, created_at),
    KEY idx_order_status_log_no (order_no),
    KEY idx_order_status_log_operator (community_id, operator_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态日志表';

-- 抢单记录表：记录志愿者抢公共池订单的过程。
CREATE TABLE IF NOT EXISTS order_grab_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '抢单记录主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '预约单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '预约单号',
    volunteer_user_id BIGINT UNSIGNED NOT NULL COMMENT '抢单志愿者用户ID',
    grab_status TINYINT NOT NULL COMMENT '抢单结果：1成功，2失败，3重复抢单，4时间冲突',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求幂等ID，用于防重复提交',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_grab_once (community_id, order_id, volunteer_user_id),
    UNIQUE KEY uk_order_grab_request (community_id, request_id),
    KEY idx_order_grab_record_order (community_id, order_id, created_at),
    KEY idx_order_grab_record_volunteer (community_id, volunteer_user_id, created_at),
    KEY idx_order_grab_record_status (community_id, grab_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抢单记录表';

-- 公共池抢单并发安全的核心 SQL 示例：
-- UPDATE service_order
-- SET order_status = 'ASSIGNED',
--     assigned_volunteer_user_id = ?,
--     assigned_at = NOW(),
--     version = version + 1
-- WHERE id = ?
--   AND community_id = ?
--   AND assign_mode = 'PUBLIC_POOL'
--   AND order_status = 'WAITING_GRAB'
--   AND assigned_volunteer_user_id IS NULL;
-- 只有受影响行数为 1，才表示抢单成功。

-- =========================================================
-- notify_db：通知记录
-- =========================================================
USE notify_db;

-- 通知记录表：记录订单状态变化后的通知发送情况。
CREATE TABLE IF NOT EXISTS notify_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知记录主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
    business_type VARCHAR(32) NOT NULL COMMENT '业务类型：ORDER_STATUS订单状态通知',
    business_id BIGINT UNSIGNED NOT NULL COMMENT '业务ID，例如预约单ID',
    receiver_user_id BIGINT UNSIGNED NOT NULL COMMENT '接收人用户ID',
    receiver_phone VARCHAR(20) DEFAULT NULL COMMENT '接收人手机号',
    notify_channel VARCHAR(32) NOT NULL COMMENT '通知渠道：SMS短信，WECHAT小程序订阅消息，IN_APP站内',
    notify_title VARCHAR(128) NOT NULL COMMENT '通知标题',
    notify_content VARCHAR(1024) NOT NULL COMMENT '通知内容',
    notify_status TINYINT NOT NULL DEFAULT 1 COMMENT '通知状态：1待发送，2发送成功，3发送失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    sent_at DATETIME DEFAULT NULL COMMENT '发送成功时间',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    mq_message_id VARCHAR(128) DEFAULT NULL COMMENT 'MQ消息ID，用于消费幂等',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notify_mq_message (mq_message_id),
    KEY idx_notify_business (community_id, business_type, business_id),
    KEY idx_notify_receiver (community_id, receiver_user_id, created_at),
    KEY idx_notify_status_retry (community_id, notify_status, next_retry_time),
    KEY idx_notify_created (community_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知记录表';
