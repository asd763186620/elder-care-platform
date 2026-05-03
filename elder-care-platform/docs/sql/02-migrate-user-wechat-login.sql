-- 用户服务微信小程序登录增量迁移脚本。
-- 适用场景：你的本地 MySQL 已经执行过旧版 01-init-schema.sql，user_account 表缺少 open_id 等新字段。
-- 新环境从零初始化时，直接执行 01-init-schema.sql 即可；老环境需要再执行本文件。

-- 强制当前 SQL 会话使用 utf8mb4，避免 mysql 客户端默认 latin1 导致中文注释或后续数据乱码。
SET NAMES utf8mb4;

USE user_db;

-- 增加微信 openId 字段。
ALTER TABLE user_account
    ADD COLUMN open_id VARCHAR(128) DEFAULT NULL COMMENT '微信小程序openId，同一小程序内唯一' AFTER phone;

-- 增加微信 unionId 字段。
ALTER TABLE user_account
    ADD COLUMN union_id VARCHAR(128) DEFAULT NULL COMMENT '微信开放平台unionId，未绑定开放平台时可为空' AFTER open_id;

-- 增加当前角色字段，用于多身份账号切换。
ALTER TABLE user_account
    ADD COLUMN current_role VARCHAR(32) DEFAULT NULL COMMENT '当前启用角色，多身份账号切换时更新' AFTER account_status;

-- 增加刷新令牌版本字段，用于退出登录和强制下线。
ALTER TABLE user_account
    ADD COLUMN refresh_token_version INT NOT NULL DEFAULT 0 COMMENT '刷新令牌版本，退出登录或强制下线时递增' AFTER current_role;

-- phone 旧版本是 NOT NULL，新微信静默注册时手机号允许为空。
ALTER TABLE user_account
    MODIFY COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '登录手机号，微信首次静默登录时可为空，绑定手机号后写入';

-- 为 open_id 增加唯一索引，防止同一微信用户重复注册。
CREATE UNIQUE INDEX uk_user_account_open_id ON user_account (open_id);

-- 为 union_id 增加普通索引，方便后续开放平台账号归并。
CREATE INDEX idx_user_account_union_id ON user_account (union_id);
