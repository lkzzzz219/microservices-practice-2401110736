# Week 07：CampusTrade 服务注册与发现

林昆泽，2401110736。仓库：https://github.com/lkzzzz219/microservices-practice-2401110736

## 1. Nacos 部署、初始化与注册

继续修改 Week 06 原有的 services/user-service、product-service、order-service，基础包保持 com.zjgsu.lkz，没有重新创建工程。服务名与 [设计文档](../../microservices-design.md) 一致；保留 monolith 和旧周次记录。

三个工程统一导入 Spring Cloud 2025.1.0 和 Spring Cloud Alibaba 2025.1.0.0 BOM；Discovery Starter 不指定版本，没有单独覆盖 Nacos Client，也没有添加 Config Starter。这是当前 Boot 4.0.x 对应的版本系列，依据 [官方兼容表](https://sca.aliyun.com/en/docs/2025.x/overview/version-explain/)。

| 用途 | 宿主机地址 | 容器端口 |
| --- | --- | --- |
| Nacos 3.2.4 控制台 | http://localhost:18080/ | 8080 |
| Nacos HTTP / 初始化与管理 API | http://localhost:18849/nacos | 8848 |
| Nacos SDK gRPC | localhost:19849 | 9848 |
| 用户服务 | localhost:8081 | 8081 |
| 商品服务 | localhost:8082 | 8082 |
| 订单服务 | localhost:8083 | 8083 |

Nacos 三个发布端口均只绑定 127.0.0.1，不暴露到公网。容器内 SDK 使用 nacos:8848，自动连接 nacos:9848。宿主机调试 SDK 时设置 NACOS_SERVER_ADDR=localhost:18849（对应 gRPC 为 +1000 的 19849）。

Nacos 开启 Client、Admin 和 Console 鉴权。Token 为随机 48 字节后 Base64 编码，server identity 独立生成。真实值和管理员密码仅在被 Git 忽略的根目录 .env；.env.example 仅提交占位符。nacos-data 挂载 /home/nacos/data，保存内置 Derby 数据及管理员初始化状态；nacos-logs 保存日志。禁止为重启而删除数据卷。

从仓库根目录依次运行（需 Python 3、Docker Desktop）：

```bash
python scripts/nacos-admin.py prepare
docker compose up -d nacos
python scripts/nacos-admin.py init
# 上一步验证管理员初始化或已有密码登录成功后，再启动业务服务
docker compose up -d --build
python scripts/nacos-admin.py list
docker compose ps
```

prepare 仅补全本地 .env 中缺失的 Nacos 值，不覆盖现有密码。init 使用官方 /v3/auth/user/admin 初始化接口，随后验证登录，不打印接口返回的密码或登录 Token；已有管理员且 .env 密码不一致时应修正 .env，不会重置管理员。控制台用本地 .env 中的 NACOS_USERNAME / NACOS_PASSWORD 登录；截图不要打开 .env 或展示登录返回体。

控制台进入 public 命名空间的服务列表，分组填 PERSONAL_GROUP；三个业务服务各自注册实际容器 IP、端口。点击“详情”可查看实例 IP 和端口；安全的 CLI 查询为 python scripts/nacos-admin.py list，不打印 Token。

## 2. 配置隔离与发现调用

个人分组为 PERSONAL_GROUP，与课堂 COURSE_GROUP 分开。默认命名空间 ID 为空，表示 public；若使用自定义命名空间，应先在控制台创建，再将实际 ID 写到 .env 的 NACOS_NAMESPACE，三个服务一致。NACOS_SERVER_ADDR、NACOS_USERNAME、NACOS_PASSWORD、NACOS_GROUP、NACOS_NAMESPACE 均从环境变量读取。

调用方向：商品发布 → user-service 校验卖家；订单创建 → user-service 校验买家 → product-service 预订商品 → 本地保存订单。取消和成交也通过发现的 product-service 地址释放或售出商品。数据库仍分别归属于 users、products、orders 服务，没有跨库读写或新增外键。

两个消费者的 RemoteApi 每次操作执行 DiscoveryClient.getInstances(service)，从当前已知列表选一个实例，用 getUri() 拼接原业务路径发送 RestTemplate HTTP 请求。没有启动时永久缓存 URI，没有固定业务地址兜底，也没有把普通 RestTemplate 的 URL 直接写成服务名。已移除 USER_SERVICE_URL、PRODUCT_SERVICE_URL 和 clients.user-url/product-url 配置。

连接 / 读取超时保留为 2000 / 3000ms，可由 HTTP_CONNECT_TIMEOUT_MS、HTTP_READ_TIMEOUT_MS 覆盖。空列表返回 503；旧缓存地址连接失败或超时返回 503；资源不存在保留 404，业务冲突保留 409。响应的 X-Discovery-user-service / X-Discovery-product-service 显示真实采用的实例 URI，日志 DISCOVERY 记录完整方法和业务路径，不记录注册密码或 Token。

成功请求示例（需先创建真实用户、商品）：

```bash
curl -i -X POST http://localhost:8083/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"真实在售商品ID","buyerId":"真实买家ID"}'
```

预期：HTTP 201，订单 CREATED，金额取商品价格快照；商品 ON_SALE → RESERVED；买家订单数 +1。具体完整请求及数据断言由根目录烟测自动生成，使用新 UUID，不依赖固定样例 ID。

## 3. 健康、下线与恢复

```bash
bash scripts/smoke-test.sh --with-outage
# 脚本内部执行这些操作，并通过鉴权管理 API 观察注册状态
docker compose stop product-service
python scripts/nacos-admin.py wait-down product-service
docker compose start product-service
python scripts/nacos-admin.py wait-up product-service
```

同一流程的预期和断言：正常创建、取消、再次下单成交后买家累计 2 条历史订单；另建一件有效在售商品。停止 product-service，等待注册中心健康实例数为 0，使用这件新商品和有效买家下单，得到 503；订单数仍为 2。此时请求可能面对空列表，也可能面对尚未更新的旧地址连接失败，均不可继续写本地数据。恢复商品服务，等待重新注册及 /actuator/health 可访问，确认商品仍 ON_SALE；再用同样参数下单，得到 201，订单数 2 → 3，商品 ON_SALE → RESERVED。

SDK gRPC 连接存活只表明注册客户端到 Nacos 的通信状态；Nacos 健康实例表示注册中心当前可用的实例视图；Actuator /actuator/health 表示应用及其纳入检查的依赖状态。例如订单数据库连接故障可能使订单服务 Actuator DOWN，而 SDK 连接仍存活、Nacos 仍保留实例，不能假定 Nacos 默认轮询 Actuator 并自动摘除。恢复时同时核对注册状态、应用健康与真实业务请求，而非只看控制台可打开。

商品预订远端事务先提交，订单本地数据库随后提交；响应丢失、进程崩溃或本地保存失败仍可能导致“商品已锁定、订单未创建”。服务发现不消除该窗口，取消和成交同样有部分成功的可能。本周沿用人工核对 reservation_id / 订单 ID 的办法，不实现跨服务事务、自动补偿或自动重试。

## 4. 脚本与测试

```bash
# 根目录，检查真实控制台 HTTP 200 和三个 Actuator UP，再做完整业务及失败断言
bash scripts/smoke-test.sh
bash scripts/smoke-test.sh --with-outage
```

脚本检查新控制台 http://localhost:18080/，没有使用 8848/nacos 作为控制台地址；连接失败、健康非 UP、状态码错误或数据断言失败均非零退出。--with-outage 等待注册中心上下线状态并验证恢复后真实下单；EXIT trap 在失败退出时尝试恢复停止的服务。控制台和健康成功不代替业务断言。

分别进入各服务执行测试：

```bash
cd services/user-service
./mvnw test
# 新终端回到仓库根目录后：
cd services/product-service
./mvnw test
# 新终端回到仓库根目录后：
cd services/order-service
./mvnw test
```

PowerShell 将 ./mvnw 换为 .\mvnw.cmd。 IDEA 项目 SDK 与终端 JAVA_HOME 是独立设置，运行前用 mvn -version 确认 Maven 使用 JDK 25。曾因终端使用 Java 8 出现 UnsupportedClassVersionError、测试数为 0；修正终端 JAVA_HOME 为 JDK 25 后，三个服务均通过。也可在根目录 mvn -f services/<服务名>/pom.xml test。测试关闭 Discovery / Nacos 自动注册，使用 H2、Mockito DiscoveryClient 和真实 loopback HttpServer；不需要 Nacos 或 MySQL。测试包括空用户实例不写商品、空用户实例不写订单、买家校验成功但空商品实例不写订单、同一进程下一次操作重新发现，以及已有 404 / 409 / 503 / 超时规则。

## 实际运行截图

截图由本人在 IDEA 与 Nacos 控制台中手动截取。下方实测文字记录首次自动运行，截图为后续重新运行；烟测每次创建新的 UUID，因此资源 ID 可以不同。

### 第 1 题：Nacos 三服务注册列表、分组及实际 IP 和端口

![第 1 题：Nacos 三服务注册列表、分组及实际 IP 和端口](screenshots/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202026-11-08%20215922.png)

### 第 2 题：HTTP 201、CREATED 与发现的用户和商品实例地址

![第 2 题：HTTP 201、CREATED 与发现的用户和商品实例地址](screenshots/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202026-11-08%20221142.png)

### 第 3 题：应用健康、商品实例下线、503 无新增订单及恢复后 201

![第 3 题：应用健康、商品实例下线、503 无新增订单及恢复后 201](screenshots/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202026-11-08%20220329.png)

### 第 4 题：控制台、健康及业务烟测 PASS

![第 4 题：控制台、健康及业务烟测 PASS](screenshots/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202026-11-08%20220447.png)

### 第 4 题：JDK 25 下三个服务的 2、4、8 个测试全部通过

![第 4 题：JDK 25 下三个服务的 2、4、8 个测试全部通过](screenshots/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202026-11-08%20221615.png)

## 2026-11-08 实际执行结果

运行结论、请求状态码、数据变化及测试摘要统一记录在本页，不另设日志或报告文件。三个服务 2 + 4 + 8 = 14 个测试全部成功。控制台根地址首次返回 302，脚本已显式跟随最多 5 次跳转，确认最终 HTTP 200；不会把任意 302 当作成功。

- Nacos 管理员首次初始化并登录验证成功；未把返回的密码或 Token 写入日志。
- PERSONAL_GROUP / public 中 user-service 为 172.20.0.8:8081、product-service 为 172.20.0.6:8082、order-service 为 172.20.0.7:8083，三个健康实例数均为 1。IP 为本次容器实际值，重建后以查询结果为准。
- 三个 Actuator 接口 HTTP 200、status=UP。正常订单创建 HTTP 201，X-Discovery 响应头显示实际用户和商品实例 URI；取消、成交及 404、409 断言也通过。
- 本次买家为 5ab069a7-c166-41c3-90dd-6f3b60f14bfb。下线测试使用新商品 845c10d8-ab64-465b-982e-37d2fb492556。
- 停止商品服务后，注册查询 healthy instances=0；下单 HTTP 503，detail 为 No available instance: product-service，买家订单数仍为 2。
- 恢复后注册查询 healthy instances=1；原新商品仍 ON_SALE。再次下单 HTTP 201，订单 3693b529-b812-4cb8-8447-384cb33eb288 为 CREATED，订单数变为 3，商品变为 RESERVED。烟测 --with-outage 退出码 0。
- 设置 NACOS_CONSOLE_URL=http://127.0.0.1:1/ 后运行烟测，curl 连接失败，脚本退出码为 7，没有输出最终 PASS，验证检查失败不会被忽略。

- 另执行 docker compose up -d --no-build --force-recreate nacos 重建 Nacos 容器，随后仅调用登录接口验证原管理员可登录，没有重新初始化管理员；三个服务再次自动注册，健康实例数各为 1，证明 nacos-data 保留管理员数据。
