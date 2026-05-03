-- 修复演示数据中文乱码脚本。
-- 适用场景：旧版 init.sql 通过默认 latin1 的 mysql 客户端导入，导致固定演示数据变成乱码。
-- 说明：本脚本只 UPDATE 固定演示数据 ID，不清空表，不影响你真实微信登录生成的新用户。

SET NAMES utf8mb4;

USE community_db;

UPDATE community
SET community_name = '幸福里社区',
    province = '浙江省',
    city = '杭州市',
    district = '西湖区',
    address = '幸福路 88 号',
    contact_name = '社区管理员'
WHERE community_id = 1;

UPDATE service_item
SET item_name = '陪诊服务',
    item_desc = '陪同老人去医院挂号、候诊、取药。'
WHERE community_id = 1 AND id = 1;

UPDATE service_item
SET item_name = '上门助餐',
    item_desc = '为老人提供送餐、取餐和简单用餐协助。'
WHERE community_id = 1 AND id = 2;

UPDATE service_item
SET item_name = '居家清洁',
    item_desc = '提供基础居家清洁和整理服务。'
WHERE community_id = 1 AND id = 3;

USE user_db;

UPDATE user_account
SET nickname = '张爷爷', real_name = '张建国'
WHERE community_id = 1 AND id = 101;

UPDATE user_account
SET nickname = '李奶奶', real_name = '李秀英'
WHERE community_id = 1 AND id = 102;

UPDATE user_account
SET nickname = '张爷爷家属', real_name = '张小明'
WHERE community_id = 1 AND id = 201;

UPDATE user_account
SET nickname = '志愿者小王', real_name = '王志愿'
WHERE community_id = 1 AND id = 301;

UPDATE user_account
SET nickname = '志愿者小陈', real_name = '陈志愿'
WHERE community_id = 1 AND id = 302;

UPDATE user_account
SET nickname = '志愿者小赵', real_name = '赵志愿'
WHERE community_id = 1 AND id = 303;

UPDATE user_account
SET nickname = '社区管理员', real_name = '社区管理员'
WHERE community_id = 1 AND id = 901;

UPDATE elder_profile
SET elder_name = '张建国',
    health_note = '高血压，行动较慢',
    emergency_contact_name = '张小明',
    address = '幸福里社区 1 幢 101 室'
WHERE community_id = 1 AND user_id = 101;

UPDATE elder_profile
SET elder_name = '李秀英',
    health_note = '糖尿病，需按时用药',
    emergency_contact_name = '李家属',
    address = '幸福里社区 2 幢 202 室'
WHERE community_id = 1 AND user_id = 102;

USE volunteer_db;

UPDATE volunteer_profile
SET volunteer_name = '王志愿',
    skill_tags = '陪诊,助餐'
WHERE community_id = 1 AND user_id = 301;

UPDATE volunteer_profile
SET volunteer_name = '陈志愿',
    skill_tags = '清洁,助餐'
WHERE community_id = 1 AND user_id = 302;

UPDATE volunteer_profile
SET volunteer_name = '赵志愿',
    skill_tags = '陪诊,清洁'
WHERE community_id = 1 AND user_id = 303;
