# elder-care-platform

社区养老服务预约小程序后端微服务项目，第一版目标是跑通主链路：

登录 -> 亲情号绑定老人 -> 志愿者设置时间 -> 发布预约单 -> 查询可用志愿者 -> 指定志愿者 -> 公共池抢单 -> 订单状态流转 -> MQ 通知。

## 技术栈

- JDK 17
- Spring Boot 3.3.13
- Spring Cloud 2023.0.6
- Spring Cloud Alibaba 2023.0.3.4
- Spring Cloud Gateway
- Nacos 注册中心和配置中心
- OpenFeign + Spring Cloud LoadBalancer
- MySQL 8
- Redis + Redisson
- RabbitMQ
- MyBatis-Plus 3.5.12
- JWT
- Swagger 3 / springdoc-openapi 2.6.0

## 模块

- `elder-care-gateway`：统一网关，负责路由、JWT 校验、用户信息透传。
- `elder-care-common`：统一返回、异常、常量、JWT、用户上下文、`@RepeatSubmit`、`@RequireRole`。
- `elder-care-api`：Feign Client、DTO、VO。
- `elder-care-user-service`：用户、登录、角色、老人资料、亲情号绑定。
- `elder-care-community-service`：社区、服务项目。
- `elder-care-volunteer-service`：志愿者资料、可服务时间、时间锁。
- `elder-care-order-service`：预约单、指定志愿者、公共池抢单、状态流转、MQ 生产。
- `elder-care-notify-service`：RabbitMQ 消费、通知记录、消费幂等。

## 启动步骤

1. 启动基础组件：

```bash
docker compose up -d
```

2. 初始化数据库：

```bash
docker cp docs/sql/01-init-schema.sql elder-care-mysql:/tmp/01-init-schema.sql
docker cp docs/sql/02-migrate-user-wechat-login.sql elder-care-mysql:/tmp/02-migrate-user-wechat-login.sql
docker cp docs/sql/03-fix-demo-data-charset.sql elder-care-mysql:/tmp/03-fix-demo-data-charset.sql
docker cp docs/sql/04-fix-schema-comments.sql elder-care-mysql:/tmp/04-fix-schema-comments.sql
docker cp docs/sql/init.sql elder-care-mysql:/tmp/init.sql
docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "source /tmp/01-init-schema.sql"
# 如果你是旧库升级，执行下面这条；全新库已由 01-init-schema.sql 包含这些字段，可以跳过。
docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "source /tmp/02-migrate-user-wechat-login.sql"
docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "source /tmp/init.sql"
# 如果旧版 init.sql 已经导入出中文乱码，执行下面这条修复固定演示数据。
docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "source /tmp/03-fix-demo-data-charset.sql"
# 如果旧版建表脚本导致表注释或字段注释乱码，执行下面这条修复 schema 注释。
docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "source /tmp/04-fix-schema-comments.sql"
```

3. 编译项目：

```bash
/Users/dev/maven/apache-maven-3.9.9/bin/mvn clean package -DskipTests
```

4. 按顺序启动服务：

```bash
java -jar elder-care-user-service/target/elder-care-user-service-0.0.1-SNAPSHOT.jar
java -jar elder-care-community-service/target/elder-care-community-service-0.0.1-SNAPSHOT.jar
java -jar elder-care-volunteer-service/target/elder-care-volunteer-service-0.0.1-SNAPSHOT.jar
java -jar elder-care-notify-service/target/elder-care-notify-service-0.0.1-SNAPSHOT.jar
java -jar elder-care-order-service/target/elder-care-order-service-0.0.1-SNAPSHOT.jar
java -jar elder-care-gateway/target/elder-care-gateway-0.0.1-SNAPSHOT.jar
```

服务端口：

- Gateway：`8080`
- user-service：`8101`
- community-service：`8102`
- volunteer-service：`8103`
- order-service：`8104`
- notify-service：`8105`

## 基础组件地址

- Nacos：`http://127.0.0.1:8848/nacos`
- RabbitMQ 管理台：`http://127.0.0.1:15672`，账号 `elder`，密码 `elder123456`
- MySQL：`127.0.0.1:3306`，账号 `root`，密码 `root123456`
- Redis：`127.0.0.1:6379`

## Nacos 配置

每个服务都有 `bootstrap.yml`，已配置：

- `spring.application.name`
- `spring.cloud.nacos.discovery.server-addr`
- `spring.cloud.nacos.config.server-addr`
- `spring.cloud.nacos.config.import-check.enabled=false`

第一版可以不在 Nacos 配置中心创建配置文件，服务会使用本地 `application.yml` 启动。

## 防重复提交

公共模块提供 `@RepeatSubmit`，使用 Redis Key：

```text
repeat:{userId}:{uri}:{requestHash}
```

已应用在：

- `POST /orders`
- `POST /orders/{orderId}/grab`
- `POST /orders/{orderId}/cancel`
- `POST /orders/{orderId}/complete`

## 权限校验

公共模块提供 `@RequireRole`，支持：

- `ELDER`
- `FAMILY`
- `VOLUNTEER`

已应用在：

- 老人资料接口只允许 `ELDER`
- 亲情号接口只允许 `FAMILY`
- 志愿者资料和设置时间只允许 `VOLUNTEER`
- 查询可用志愿者允许 `ELDER`、`FAMILY`
- 公共池和抢单只允许 `VOLUNTEER`
- 下单允许 `ELDER`、`FAMILY`

亲情号代下单还会通过 `user-service` 内部接口校验绑定关系。

## Postman 测试顺序

1. `POST /auth/mock-login`，亲情号登录：`userId=201`，角色 `FAMILY`
2. `GET /family/elders`，查询绑定老人，得到老人 `101`
3. `GET /service-items`，查询服务项目，得到项目 `1/2/3`
4. `GET /volunteers/available`，查询可用志愿者
5. `POST /orders`，发布指定志愿者订单，`assignMode=ASSIGNED`
6. `POST /orders`，发布公共池订单，`assignMode=PUBLIC`
7. `POST /auth/mock-login`，志愿者登录：`userId=302`，角色 `VOLUNTEER`
8. `GET /orders/pool`，查看公共订单池
9. `POST /orders/{orderId}/grab`，抢单
10. `GET /orders/my`，查询订单状态
11. `POST /orders/{orderId}/complete`，完成订单

## curl 示例

亲情号登录：

```bash
FAMILY_TOKEN=$(curl -s -X POST http://127.0.0.1:8080/auth/mock-login \
  -H 'Content-Type: application/json' \
  -d '{"userId":201,"communityId":1,"roles":["FAMILY"]}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')
```

查询绑定老人：

```bash
curl http://127.0.0.1:8080/family/elders \
  -H "Authorization: Bearer $FAMILY_TOKEN"
```

查询服务项目：

```bash
curl http://127.0.0.1:8080/service-items \
  -H "Authorization: Bearer $FAMILY_TOKEN"
```

查询可用志愿者：

```bash
curl 'http://127.0.0.1:8080/volunteers/available?serviceItemId=1&startTime=2026-05-04T10:00:00&endTime=2026-05-04T11:00:00' \
  -H "Authorization: Bearer $FAMILY_TOKEN"
```

发布指定志愿者订单：

```bash
curl -X POST http://127.0.0.1:8080/orders \
  -H "Authorization: Bearer $FAMILY_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"elderUserId":101,"serviceItemId":1,"serviceAddress":"幸福里社区 1 幢 101 室","serviceStartTime":"2026-05-04T10:00:00","serviceEndTime":"2026-05-04T11:00:00","assignMode":"ASSIGNED","specifiedVolunteerUserId":301,"remark":"陪诊指定志愿者测试"}'
```

发布公共池订单：

```bash
PUBLIC_ORDER_ID=$(curl -s -X POST http://127.0.0.1:8080/orders \
  -H "Authorization: Bearer $FAMILY_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"elderUserId":101,"serviceItemId":2,"serviceAddress":"幸福里社区 1 幢 101 室","serviceStartTime":"2026-05-04T09:00:00","serviceEndTime":"2026-05-04T10:00:00","assignMode":"PUBLIC","remark":"公共池助餐测试"}' \
  | sed -E 's/.*"id":([0-9]+).*/\1/')
```

志愿者登录：

```bash
VOLUNTEER_TOKEN=$(curl -s -X POST http://127.0.0.1:8080/auth/mock-login \
  -H 'Content-Type: application/json' \
  -d '{"userId":302,"communityId":1,"roles":["VOLUNTEER"]}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')
```

查看公共订单池：

```bash
curl http://127.0.0.1:8080/orders/pool \
  -H "Authorization: Bearer $VOLUNTEER_TOKEN"
```

抢单：

```bash
curl -X POST http://127.0.0.1:8080/orders/$PUBLIC_ORDER_ID/grab \
  -H "Authorization: Bearer $VOLUNTEER_TOKEN"
```

查询我的订单：

```bash
curl http://127.0.0.1:8080/orders/my \
  -H "Authorization: Bearer $VOLUNTEER_TOKEN"
```

完成订单：

```bash
curl -X POST http://127.0.0.1:8080/orders/$PUBLIC_ORDER_ID/complete \
  -H "Authorization: Bearer $VOLUNTEER_TOKEN"
```

## 最终验证接口

```bash
curl http://127.0.0.1:8080/actuator/health
curl http://127.0.0.1:8101/actuator/health
curl http://127.0.0.1:8102/actuator/health
curl http://127.0.0.1:8103/actuator/health
curl http://127.0.0.1:8104/actuator/health
curl http://127.0.0.1:8105/actuator/health
```

Swagger：

- 网关聚合文档：`http://127.0.0.1:8080/swagger-ui.html`
- 用户服务：`http://127.0.0.1:8101/swagger-ui.html`
- 社区服务：`http://127.0.0.1:8102/swagger-ui.html`
- 志愿者服务：`http://127.0.0.1:8103/swagger-ui.html`
- 订单服务：`http://127.0.0.1:8104/swagger-ui.html`
- 通知服务：`http://127.0.0.1:8105/swagger-ui.html`

## 常见启动报错

`No Feign Client for loadBalancing defined`：

- 原因：使用服务名调用 Feign 或 Gateway `lb://` 路由时缺少 `spring-cloud-starter-loadbalancer`。
- 处理：项目已在 `elder-care-api` 和 `elder-care-gateway` 中补充该依赖。

`LiteWebJarsResourceResolver ClassNotFoundException`：

- 原因：springdoc 2.8.x 依赖 Spring Framework 6.2，而 Spring Boot 3.3.x 使用 Spring Framework 6.1.x。
- 处理：项目已调整为 `springdoc-openapi 2.6.0`。

`Unable to connect to Redis server: 127.0.0.1:6379`：

- 原因：Redis 容器未启动，或当前运行环境禁止 Java 进程访问本机端口。
- 处理：执行 `docker compose ps` 确认 Redis 运行；在受限沙箱中需要允许 Java 访问本机网络。

`Nacos registry failed`：

- 原因：Nacos 未启动或 `NACOS_SERVER_ADDR` 配置错误。
- 处理：确认 `http://127.0.0.1:8848/nacos` 可访问；本机运行使用 `127.0.0.1:8848`。

`Access denied for user root`：

- 原因：MySQL 密码和 `application.yml` 不一致。
- 处理：本项目默认密码为 `root123456`。

中文显示乱码：

- 原因：终端或 curl 输出编码不是 UTF-8。
- 处理：数据库表和连接均为 `utf8mb4`，优先检查终端编码。

## 当前运行检查结论

- Maven 全模块 `clean package -DskipTests` 通过。
- Docker Compose 中 MySQL、Redis、RabbitMQ、Nacos 均可启动。
- MySQL 建表脚本和 `docs/sql/init.sql` 演示数据已验证可导入。
- Nacos 注册配置已验证可用。
- Gateway 路由需要 `spring-cloud-starter-loadbalancer`，已补齐。
- OpenFeign 需要 `spring-cloud-starter-loadbalancer`，已补齐。
- MyBatis-Plus Mapper 扫描已在各服务启动类配置。
- Redis / Redisson 在允许本机网络访问后可连接。
- RabbitMQ exchange / queue 由 order-service 和 notify-service 的配置类自动声明。

## 第二版小程序端增强

本次第二版在第一版主链路上继续增强小程序端闭环，不新增后台管理页面。

新增能力：

- 微信小程序登录增强：`POST /auth/wx-login` 支持 `code`、`communityId`、`loginRole`，保留 `POST /auth/mock-login`。
- Token 机制：accessToken 默认 2 小时，refreshToken 默认 30 天并存 Redis，退出登录会删除当前 refreshToken 并递增版本。
- 当前用户：`GET /auth/current` 返回当前角色、角色列表、手机号、老人档案摘要和志愿者摘要。
- 订单查询：订单详情、我的订单游标分页、公共池游标分页、订单状态数量。
- 订单状态机：`WAIT_GRAB -> WAIT_SERVICE -> IN_SERVICE -> WAIT_CONFIRM -> COMPLETED`，取消和超时关闭为终态。
- 志愿者工作台：今日签到、签到状态、签到记录分页、工作台摘要。
- 消息中心：我的消息分页、未读数、单条已读、全部已读。
- 评价体系：订单完成后评价志愿者，订单唯一评价防重。
- MQ Outbox：订单事务内写 `order_event_outbox`，定时任务异步发送 RabbitMQ，失败自动重试。
- 网关增强：重点接口 Redis 限流、访问日志、慢接口 warn 日志。

新增数据库脚本：

```bash
docker cp docs/sql/05-v2-miniapp-enhancement.sql elder-care-mysql:/tmp/05-v2-miniapp-enhancement.sql
docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "source /tmp/05-v2-miniapp-enhancement.sql"
```

第二版新增表：

- `order_db.order_event_outbox`
- `order_db.order_evaluation`
- `volunteer_db.volunteer_checkin_record`
- `volunteer_db.volunteer_time_compensation`

第二版新增接口：

```text
POST /auth/wx-login
POST /auth/refresh-token
POST /auth/logout
GET  /auth/current
POST /auth/switch-role

GET  /orders/{orderId}
GET  /orders/my/page
GET  /orders/pool/page
GET  /orders/status-count
POST /orders/{orderId}/start
POST /orders/{orderId}/submit-complete
POST /orders/{orderId}/confirm
POST /orders/{orderId}/evaluate
GET  /volunteers/{volunteerId}/reviews
GET  /volunteers/{volunteerId}/score

POST /volunteers/check-in
GET  /volunteers/check-in/today
GET  /volunteers/check-in/page
GET  /volunteers/workbench

GET  /notices/my/page
GET  /notices/unread-count
POST /notices/{noticeId}/read
POST /notices/read-all
```

推荐第二版测试链路：

```bash
# 1. 微信登录老人，真实小程序传 wx.login() 返回的 code。
curl -X POST http://127.0.0.1:8080/auth/wx-login \
  -H "Content-Type: application/json" \
  -d '{"code":"wx-code-elder","communityId":1,"loginRole":"ELDER"}'

# 2. 微信登录亲情号。
curl -X POST http://127.0.0.1:8080/auth/wx-login \
  -H "Content-Type: application/json" \
  -d '{"code":"wx-code-family","communityId":1,"loginRole":"FAMILY"}'

# 3. 查看当前用户。
curl http://127.0.0.1:8080/auth/current \
  -H "Authorization: Bearer $TOKEN"

# 4. 志愿者登录、完善资料、签到。
curl -X POST http://127.0.0.1:8080/volunteers/check-in \
  -H "Authorization: Bearer $VOLUNTEER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"longitude":116.397128,"latitude":39.916527,"address":"社区服务站"}'

# 5. 我的订单分页。
curl "http://127.0.0.1:8080/orders/my/page?size=10" \
  -H "Authorization: Bearer $TOKEN"

# 6. 公共订单池分页。
curl "http://127.0.0.1:8080/orders/pool/page?size=10" \
  -H "Authorization: Bearer $VOLUNTEER_TOKEN"

# 7. 抢单、开始服务、提交完成、确认完成。
curl -X POST http://127.0.0.1:8080/orders/$ORDER_ID/grab -H "Authorization: Bearer $VOLUNTEER_TOKEN"
curl -X POST http://127.0.0.1:8080/orders/$ORDER_ID/start -H "Authorization: Bearer $VOLUNTEER_TOKEN"
curl -X POST http://127.0.0.1:8080/orders/$ORDER_ID/submit-complete -H "Authorization: Bearer $VOLUNTEER_TOKEN"
curl -X POST http://127.0.0.1:8080/orders/$ORDER_ID/confirm -H "Authorization: Bearer $ELDER_TOKEN"

# 8. 评价志愿者。
curl -X POST http://127.0.0.1:8080/orders/$ORDER_ID/evaluate \
  -H "Authorization: Bearer $ELDER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"score":5,"tags":"准时,耐心","content":"服务很好","anonymous":false}'

# 9. 消息中心。
curl "http://127.0.0.1:8080/notices/my/page?size=10" -H "Authorization: Bearer $ELDER_TOKEN"
curl http://127.0.0.1:8080/notices/unread-count -H "Authorization: Bearer $ELDER_TOKEN"
```

并发和一致性说明：

- 抢单仍使用 `lock:order:grab:{orderId}` Redisson 锁，并保留 MySQL 条件更新 `where order_status = 'WAIT_GRAB'`，所以并发抢单只有一个请求能更新成功。
- 发布订单、抢单、取消、签到、评价等接口使用 `@RepeatSubmit` 或数据库唯一索引防重复。
- 志愿者时间冲突继续使用 Redisson 锁 + MySQL 唯一索引双保险。
- 订单事件不再在业务事务里直接发 MQ，而是先写 `order_event_outbox`；事务提交后定时任务发送，失败进入 `FAILED` 并按 `next_retry_time` 重试。
- notify-service 消费时继续按 `mq_message_id/eventId` 唯一索引做幂等，避免重复通知。
