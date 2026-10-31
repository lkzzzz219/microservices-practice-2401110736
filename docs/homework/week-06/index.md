# Week 06：服务拆分、HTTP 协作与本地编排

姓名：林昆泽；学号：2401110736；项目：CampusTrade 校园二手交易平台。

仓库链接：<https://github.com/lkzzzz219/microservices-practice-2401110736>

本周运行、各服务测试和配置说明集中在新增的 [services/README.md](../../../services/README.md)。原 README、环境变量模板、忽略文件、旧周次文档和单体代码保持原样；已有文件中仅按本周要求更新根目录 compose.yaml，旧编排完整保存为 compose.monolith.yaml。

## 1. 服务拆分设计

按照用户、商品、订单的业务生命周期拆成三个服务，每个服务独立启动、独立数据库和账号。保留 `monolith/` 原工程，新增根目录 `services/`，没有把单体改名或覆盖。原单体已实现商品和订单，用户服务是对原选题中用户模块的最小补齐，只提供创建和查询，不声称实现了登录。

完整的模块映射、职责、端口、数据表、调用关系、Mermaid 架构图和顺序图见 [microservices-design.md](../../microservices-design.md)。

| 服务 | 端口 | 数据库 / 账号 | 数据表 | 依赖 |
| --- | --- | --- | --- | --- |
| user-service | 8081 | campustrade_users / users_app | users | 无 |
| product-service | 8082 | campustrade_products / products_app | items | HTTP 查询卖家 |
| order-service | 8083 | campustrade_orders / orders_app | trade_orders | HTTP 查询买家、预订/释放/售出商品 |

每个服务仅有自己的 Repository；跨服务只保存 ID。商品价格在下单时复制为订单金额快照。商品锁绑定订单 ID，由商品服务本地事务与行锁维护，订单服务不读写商品数据库。

## 2. 代码拆分

每个 `services/<服务名>/` 都有独立 pom.xml、src、application.yml、启动类、controller、service、repository 和测试。基础包为 `com.zjgsu.lkz`。测试使用 H2，不依赖运行中的 MySQL 或其他服务；商品和订单测试使用 JDK HttpServer 提供真实 loopback HTTP 响应，以验证 RestTemplate 的序列化、错误映射和超时。

仓库根目录运行：

```bash
mvn -f services/user-service/pom.xml test
mvn -f services/product-service/pom.xml test
mvn -f services/order-service/pom.xml test
mvn -f monolith/pom.xml test
```

没有全局 Maven 时，进入各服务目录，执行 `./mvnw test`；PowerShell 使用 `.\mvnw.cmd test`。

2026-10-31 验证：用户服务 2 个、商品服务 3 个、订单服务 5 个测试全部通过（共 10 个，0 failures / 0 errors）。测试覆盖用户创建查询、卖家不存在时不写商品、自购拒绝、商品锁归属、锁定释放成交、HTTP 预订后写订单、404 / 409 / 503 和读取超时时订单数为零。

保留的单体工程另有 28 个回归测试全部通过；`git diff -- monolith` 无变化。

服务地址来自 application.yml，可由 USER_SERVICE_URL、PRODUCT_SERVICE_URL 覆盖；连接超时默认 2000ms，读取超时默认 3000ms，可通过 HTTP_CONNECT_TIMEOUT_MS、HTTP_READ_TIMEOUT_MS 覆盖。未忽略远端错误：404 表示依赖资源不存在，409 表示状态冲突，503 表示依赖不可用或超时，其他不合预期的远端拒绝为 502。

## 3. 本地编排

根目录 compose.yaml 编排三个 Spring Boot 服务与三个 MySQL 8.4.11 容器。数据库分别持久化到 users-data、products-data、orders-data；每个应用仅持有自己数据库的普通账号。容器通过 user-service、product-service、users-db 等 Compose DNS 名通信。数据库无宿主机端口映射；localhost 只在本容器健康检查中出现。

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=50 order-service
# 停止后保留数据库卷
docker compose down
docker compose up -d
```

Dockerfile 使用 Maven / JRE 多阶段构建。应用等待自己的 MySQL 健康后启动；应用探针调用 /actuator/health，数据库探针用各自应用账号执行 SELECT 1。新增 `services/.env.example` 说明六个可覆盖的密码，原根目录 `.env.example` 保留不变；默认值面向本机教学演示。旧单体编排保存在 compose.monolith.yaml，可用 `docker compose -f compose.monolith.yaml up -d --build` 单独运行。

## 4. 验证

仓库根目录执行，需要 Bash、curl、Python 3；Windows 可用 Git Bash：

```bash
bash scripts/smoke-test.sh
# 额外验证实际依赖服务停机；会短暂停止并恢复 product-service
bash scripts/smoke-test.sh --with-outage
```

脚本先检查三个健康接口，再创建独立的卖家、买家、商品，自动提取返回 ID。每次运行使用新资源，不依赖固定 ID、不清空旧数据。任何状态码或数据断言不符合预期都会以非零退出码结束。脚本使用退出 trap 恢复在停机场景中停止的商品服务。

| 顺序 | 请求（ID 使用本次创建结果） | 预期状态 | 数据变化 / 断言 |
| --- | --- | --- | --- |
| 1 | 三个 GET /actuator/health | 200 | status=UP |
| 2 | POST :8081/api/users，displayName 为 smoke-seller / smoke-buyer | 201 | 新增两名用户 |
| 3 | POST :8082/api/items，sellerId、title、description、price=12.50 | 201 | 新增商品 ON_SALE |
| 4 | POST :8083/api/orders，itemId、buyerId | 201 | CREATED 订单 +1；商品 RESERVED；金额快照 12.50 |
| 5 | 对同一商品再次下单 | 409 | 买家订单总数仍为 1 |
| 6 | POST /api/orders/{id}/cancel | 200 | 订单 CANCELLED；商品 ON_SALE |
| 7 | 同一商品重新下单，然后 POST /api/orders/{id}/complete | 201 / 200 | 总订单数为 2（含取消订单）；新订单 COMPLETED；商品 SOLD |
| 8 | itemId=missing-item 下单 | 404 | 买家订单数保持 2 |
| 9 | buyerId=missing-user 下单 | 404 | 买家订单数保持 2 |
| 10 | 新建第二件商品，以卖家 ID 自购 | 409 | 商品仍为 ON_SALE |
| 11 | 停止 product-service，再对第二件商品下单 | 503 | 买家订单数保持 2 |
| 12 | 恢复 product-service，GET 第二件商品 | 200 | 商品仍 ON_SALE，未被失败请求预订 |

请求示例：

```bash
curl -i -X POST http://localhost:8081/api/users \
  -H 'Content-Type: application/json' -d '{"displayName":"买家"}'
curl -i -X POST http://localhost:8083/api/orders \
  -H 'Content-Type: application/json' -d '{"itemId":"真实商品ID","buyerId":"真实买家ID"}'
```

### 一致性窗口

下单顺序是校验买家 → 商品服务提交 RESERVED → 订单服务提交 CREATED。如果远端预订成功但响应丢失、本地服务崩溃或本地数据库提交失败，会出现商品已锁定但订单不存在。取消或成交也可能出现商品状态已变更、本地订单状态仍未提交。`@Transactional` 只保证本地数据库事务，无法自动回滚另一个服务。当前可按 reservation_id / 订单 ID 人工核对处理；本周不实现跨服务原子事务、自动补偿、消息或自动重试。

### 2026-10-31 实际运行结果

在 Docker Desktop Linux 引擎上实际执行 `docker compose up -d --build`，三个应用和三个 MySQL 成功启动。执行 `bash scripts/smoke-test.sh --with-outage`，退出码为 0；完整真实输出保存在 [verification.log](verification.log)，未使用模拟运行截图。

- 买家 ID：`2c2bef50-c3e1-43ae-8bae-8b164495702e`。
- 商品 ID：`a42ff66e-0dc5-41c5-92fa-8be47d2c6b92`；首次下单 201，重复下单 409，取消后 ON_SALE，重新下单并成交后 SOLD。
- 订单 `fa89dde3-172f-4dea-94db-1cab541949fe` 为 CANCELLED；订单 `08a3f59e-abad-4cdd-9155-61685522cff7` 为 COMPLETED，金额均为 12.50。
- 商品或买家不存在均返回 404；未知买家的订单列表为空，正常买家的订单数保持 2。
- 实际停止 product-service 后下单返回 503，detail 为 `Dependency unavailable or timed out`，正常买家的订单数保持 2。
- 恢复后第二件商品 `ed8031fa-9c07-4a1a-af33-f3403371c785` 仍为 ON_SALE，说明失败请求没有预订商品。
- 使用三个数据库的应用账号分别执行 `SELECT DATABASE(), CURRENT_USER(); SHOW TABLES;`，得到 `campustrade_users / users_app / users`、`campustrade_products / products_app / items`、`campustrade_orders / orders_app / trade_orders`。
- 执行 `docker compose up -d --force-recreate products-db product-service` 重建商品数据库及应用容器后，第二件商品仍可查询且状态为 ON_SALE，验证命名卷保留数据；最终六个容器全部 healthy。

日志中启动探测阶段出现短暂 `Empty reply from server`，随后重试成功；它发生于应用启动或恢复期间，最终业务断言全部通过。容器数据中的 LocalDateTime 使用容器默认 UTC；此文验证日期采用北京时间。

## 实际运行截图

以下为本人在 IDEA 终端中重新运行后截取的结果；每次烟测创建新的 ID，因此截图与前面 verification.log 的资源 ID 可以不同。

### 代码拆分：三个服务共 10 个测试全部通过

![代码拆分：三个服务共 10 个测试全部通过](screenshots/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202026-10-31%20193638.png)

### 本地编排：三个应用和三个 MySQL 均为 healthy

![本地编排：三个应用和三个 MySQL 均为 healthy](screenshots/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202026-10-31%20193715.png)

### 验证：健康接口、下单、取消与成交成功

![验证：健康接口、下单、取消与成交成功](screenshots/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202026-10-31%20193824.png)

### 验证：404、409、503 及失败后订单数不变

![验证：404、409、503 及失败后订单数不变](screenshots/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202026-10-31%20193847.png)
