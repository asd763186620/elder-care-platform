# 接口测试示例

本文件和 README 中的 curl 示例一致，建议先执行：

```bash
docker compose up -d
docker exec elder-care-mysql mysql -uroot -proot123456 -e "source /tmp/01-init-schema.sql"
docker exec elder-care-mysql mysql -uroot -proot123456 -e "source /tmp/init.sql"
```

测试账号：

- 老人：`101`、`102`
- 亲情号：`201`
- 志愿者：`301`、`302`、`303`
- 社区：`1`
- 服务项目：`1` 陪诊，`2` 助餐，`3` 清洁

推荐按 README 的 Postman 顺序执行完整链路。
