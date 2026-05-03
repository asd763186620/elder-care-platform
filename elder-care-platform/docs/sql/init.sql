-- 社区养老服务预约小程序第一版演示数据。
-- 使用方式：先执行 docs/sql/01-init-schema.sql 建表，再执行本文件插入测试数据。
-- 说明：本文件使用固定主键，便于 Postman 和 curl 示例直接引用。

-- 强制当前 SQL 会话使用 utf8mb4，避免 mysql 客户端默认 latin1 导致中文演示数据乱码。
SET NAMES utf8mb4;

-- =========================================================
-- 清理旧演示数据，保证脚本可以重复执行
-- =========================================================

USE notify_db;
DELETE FROM notify_record WHERE community_id = 1;

USE order_db;
DELETE FROM order_grab_record WHERE community_id = 1;
DELETE FROM order_status_log WHERE community_id = 1;
DELETE FROM service_order WHERE community_id = 1;

USE volunteer_db;
DELETE FROM volunteer_time_lock WHERE community_id = 1;
DELETE FROM volunteer_available_time WHERE community_id = 1;
DELETE FROM volunteer_profile WHERE community_id = 1;

USE community_db;
DELETE FROM service_item WHERE community_id = 1;
DELETE FROM community WHERE community_id = 1;

USE user_db;
DELETE FROM family_elder_bind WHERE community_id = 1;
DELETE FROM elder_profile WHERE community_id = 1;
DELETE FROM user_role WHERE community_id = 1;
DELETE FROM user_account WHERE community_id = 1;

-- =========================================================
-- community_db：一个社区，三个服务项目
-- =========================================================

USE community_db;

INSERT INTO community (
    id, community_id, community_name, province, city, district, address,
    contact_name, contact_phone, community_status, deleted
) VALUES (
    1, 1, '幸福里社区', '浙江省', '杭州市', '西湖区', '幸福路 88 号',
    '社区管理员', '057100000001', 1, 0
);

INSERT INTO service_item (
    id, community_id, item_name, item_code, item_desc,
    duration_minutes, price_cent, item_status, deleted
) VALUES
    (1, 1, '陪诊服务', 'MEDICAL_COMPANY', '陪同老人去医院挂号、候诊、取药。', 120, 0, 1, 0),
    (2, 1, '上门助餐', 'MEAL_ASSIST', '为老人提供送餐、取餐和简单用餐协助。', 60, 0, 1, 0),
    (3, 1, '居家清洁', 'HOME_CLEAN', '提供基础居家清洁和整理服务。', 90, 0, 1, 0);

-- =========================================================
-- user_db：两个老人，一个亲情号，三个志愿者
-- =========================================================

USE user_db;

INSERT INTO user_account (
    id, community_id, phone, open_id, union_id, nickname, real_name, gender, account_status, current_role, refresh_token_version, deleted
) VALUES
    (101, 1, '18800000101', 'mock_openid_elder_101', 'mock_unionid_elder_101', '张爷爷', '张建国', 1, 1, 'ELDER', 0, 0),
    (102, 1, '18800000102', 'mock_openid_elder_102', 'mock_unionid_elder_102', '李奶奶', '李秀英', 2, 1, 'ELDER', 0, 0),
    (201, 1, '18800000201', 'mock_openid_family_201', 'mock_unionid_family_201', '张爷爷家属', '张小明', 1, 1, 'FAMILY', 0, 0),
    (301, 1, '18800000301', 'mock_openid_volunteer_301', 'mock_unionid_volunteer_301', '志愿者小王', '王志愿', 1, 1, 'VOLUNTEER', 0, 0),
    (302, 1, '18800000302', 'mock_openid_volunteer_302', 'mock_unionid_volunteer_302', '志愿者小陈', '陈志愿', 2, 1, 'VOLUNTEER', 0, 0),
    (303, 1, '18800000303', 'mock_openid_volunteer_303', 'mock_unionid_volunteer_303', '志愿者小赵', '赵志愿', 1, 1, 'VOLUNTEER', 0, 0),
    (901, 1, '18800000901', 'mock_openid_admin_901', 'mock_unionid_admin_901', '社区管理员', '社区管理员', 1, 1, 'ADMIN', 0, 0);

INSERT INTO user_role (
    community_id, user_id, role_code, role_status, deleted
) VALUES
    (1, 101, 'ELDER', 1, 0),
    (1, 102, 'ELDER', 1, 0),
    (1, 201, 'FAMILY', 1, 0),
    (1, 301, 'VOLUNTEER', 1, 0),
    (1, 302, 'VOLUNTEER', 1, 0),
    (1, 303, 'VOLUNTEER', 1, 0),
    (1, 901, 'ADMIN', 1, 0);

INSERT INTO elder_profile (
    community_id, user_id, elder_name, elder_phone, age, address,
    health_note, emergency_contact_name, emergency_contact_phone,
    profile_status, deleted
) VALUES
    (1, 101, '张建国', '18800000101', 76, '幸福里社区 1 幢 101 室', '高血压，行动较慢', '张小明', '18800000201', 1, 0),
    (1, 102, '李秀英', '18800000102', 72, '幸福里社区 2 幢 202 室', '糖尿病，需按时用药', '李家属', '18800000202', 1, 0);

INSERT INTO family_elder_bind (
    community_id, family_user_id, elder_user_id, relationship,
    bind_status, bind_source, confirmed_at, deleted
) VALUES (
    1, 201, 101, 'CHILD', 2, 'MINI_APP', NOW(), 0
);

-- =========================================================
-- volunteer_db：三个志愿者资料和可服务时间
-- =========================================================

USE volunteer_db;

INSERT INTO volunteer_profile (
    community_id, user_id, volunteer_name, volunteer_phone,
    skill_tags, service_radius_meter, profile_status, audit_time, deleted
) VALUES
    (1, 301, '王志愿', '18800000301', '陪诊,助餐', 3000, 2, NOW(), 0),
    (1, 302, '陈志愿', '18800000302', '清洁,助餐', 2500, 2, NOW(), 0),
    (1, 303, '赵志愿', '18800000303', '陪诊,清洁', 5000, 2, NOW(), 0);

INSERT INTO volunteer_available_time (
    community_id, volunteer_user_id, service_item_id, available_date,
    start_time, end_time, available_status, deleted
) VALUES
    (1, 301, 1, '2026-05-04', '2026-05-04 09:00:00', '2026-05-04 12:00:00', 1, 0),
    (1, 301, 2, '2026-05-04', '2026-05-04 14:00:00', '2026-05-04 17:00:00', 1, 0),
    (1, 302, 2, '2026-05-04', '2026-05-04 09:00:00', '2026-05-04 12:00:00', 1, 0),
    (1, 302, 3, '2026-05-04', '2026-05-04 14:00:00', '2026-05-04 18:00:00', 1, 0),
    (1, 303, 1, '2026-05-04', '2026-05-04 10:00:00', '2026-05-04 13:00:00', 1, 0),
    (1, 303, 3, '2026-05-04', '2026-05-04 09:00:00', '2026-05-04 12:00:00', 1, 0);
