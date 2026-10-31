# CampusTrade Week 06 微服务运行说明

## 快速开始（Week 06 微服务拆分）

保留 `monolith/` 单体版本；`services/user-service`、`services/product-service`、`services/order-service` 是三个独立 Spring Boot 4.0.8 / Java 25 工程，基础包 `com.zjgsu.lkz`。设计见 [微服务设计](../docs/microservices-design.md)，本周实现与验证见 [Week 06](../docs/homework/week-06/index.md)。

### Docker 一键启动

需要运行中的 Docker Desktop。镜像在容器内编译，无需本机 Java/Maven。以下所有命令均在仓库根目录执行：

```bash
# 本地教学默认密码可直接运行；六个密码的变量示例见 services/.env.example，可自行设置环境变量覆盖
docker compose up -d --build
docker compose ps
docker compose logs -f order-service
# 需要 Bash、curl、Python 3；Windows 使用 Git Bash 执行
bash scripts/smoke-test.sh
# 额外停机验证：短暂停止商品服务，验证 503 和无新增订单，退出时恢复
bash scripts/smoke-test.sh --with-outage
docker compose down           # 保留三个命名数据卷
```

端口：用户 8081、商品 8082、订单 8083；三个健康接口均为 `/actuator/health`。三个 MySQL 分别使用独立数据库、应用账号和卷，不发布数据库端口。容器通过服务名访问其他容器。默认密码仅用于本地作业；已有数据卷的密码不会因为修改 .env 自动改变。

### 测试（不依赖 MySQL 或 Docker）

本机需 JDK 25、Maven。仓库根目录分别执行：

```bash
mvn -f services/user-service/pom.xml test
mvn -f services/product-service/pom.xml test
mvn -f services/order-service/pom.xml test
# 旧单体回归测试
mvn -f monolith/pom.xml test
```

未安装 Maven 时，每个服务都保留 Wrapper：进入对应 `services/<服务名>/`，运行 `./mvnw test`，Windows PowerShell 运行 `.\mvnw.cmd test`。用户服务 2 个、商品服务 3 个、订单服务 5 个测试；HTTP 依赖使用本机真实 HTTP 测试服务器，数据库使用隔离的 H2。

### 本机开发

MySQL 模式：分别准备设计文档中的三个库和账号，每个终端设置自己的 DB_URL、DB_USER、DB_PASSWORD，再运行 `mvn -f services/<服务名>/pom.xml spring-boot:run`。默认服务地址为 `http://localhost:8081`、`http://localhost:8082`，可通过 USER_SERVICE_URL、PRODUCT_SERVICE_URL 覆盖；连接和读取超时分别通过 HTTP_CONNECT_TIMEOUT_MS、HTTP_READ_TIMEOUT_MS 设置，默认 2000/3000ms。

也可使用独立 H2 文件库演示，在三个终端分别执行（根目录）：

```bash
mvn -f services/user-service/pom.xml -P local-demo spring-boot:run -Dspring-boot.run.profiles=local
mvn -f services/product-service/pom.xml -P local-demo spring-boot:run -Dspring-boot.run.profiles=local
mvn -f services/order-service/pom.xml -P local-demo spring-boot:run -Dspring-boot.run.profiles=local
```

H2 模式只用于免 MySQL 的本机演示，数据在运行工作目录的 `.data/` 中，Compose 默认仍为三个 MySQL。PowerShell 如对 `-D` 解析有问题，可将完整参数用单引号包围。

### 接口和数据流

先 `POST http://localhost:8081/api/users`，请求 `{"displayName":"卖家"}`，再创建买家；使用返回的真实 ID 发布商品和下单，不再接受单体示例中的任意 u-seller / u-buyer 字符串。

| 服务 | 接口 | 结果 |
| --- | --- | --- |
| 用户 | POST /api/users；GET /api/users/{id} | 创建 201；查询 200 / 404 |
| 商品 | POST /api/items；GET /api/items；GET /api/items/{id} | 发布前 HTTP 校验卖家；201 / 200 / 404 |
| 商品 | POST /api/items/{id}/reserve、/release、/sell | 商品状态由商品服务维护；200 / 404 / 409 |
| 订单 | POST /api/orders；GET /api/orders/{id}；GET /api/orders?buyerId=... | 下单前 HTTP 校验买家、预订商品；201 / 200 |
| 订单 | POST /api/orders/{id}/cancel、/complete | HTTP 释放 / 售出商品，再更新本地订单；200 / 404 / 409 |
| 全部 | GET /actuator/health | 200，status=UP |

下单请求保持 `{"itemId":"真实商品ID","buyerId":"真实买家ID"}`。依赖资源不存在返回 404、状态冲突返回 409、依赖不可用或超时返回 503，校验失败不新增订单。远端成功后本地提交失败仍可能产生不一致，具体窗口和人工核对方式见设计文档；本周未实现分布式事务或自动补偿。


原根目录 README 保留 Week 05 内容；其中旧单体的 docker compose 启停命令请显式加 `-f compose.monolith.yaml`。不加 -f 的根目录 compose.yaml 现在按作业要求启动微服务。
