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
docker cp docs/sql/init.sql elder-care-mysql:/tmp/init.sql
docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "source /tmp/01-init-schema.sql"
# 如果你是旧库升级，执行下面这条；全新库已由 01-init-schema.sql 包含这些字段，可以跳过。
docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "source /tmp/02-migrate-user-wechat-login.sql"
docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "source /tmp/init.sql"
# 如果旧版 init.sql 已经导入出中文乱码，执行下面这条修复固定演示数据。
docker exec elder-care-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "source /tmp/03-fix-demo-data-charset.sql"
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
