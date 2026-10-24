# Week 04：单体应用与数据库持久化

> 作业目标：在 Week 03 的 Spring Boot 工程上实现个人选题（CampusTrade 校园二手交易平台）的一条完整业务流程——**买家下单到取消/成交的商品状态联动**，包含模型、Service 业务规则、REST API 与 MySQL 持久化。代码全部在 `monolith/` 内，包名 `com.zjgsu.linkunze`。

## 1. 业务建模

**设计**：本周核心流程为「买家对在售商品下单 → 订单创建、商品锁定 → 取消（商品回在售）或确认成交（商品售出）」。参与实体为商品 `Item` 与订单 `TradeOrder`（多对一：一个商品任意时刻最多一条进行中订单），关系通过 `Order.itemId` 逻辑外键维系，订单不直接持有商品对象。Service 层业务规则：商品必须存在（404）、必须 ON_SALE（409）、买家不能是卖家本人（409）、同商品不能有第二条进行中订单（409），通过后创建订单并锁定商品，同事务写入。

**实现思路**：沿用课堂实践验证过的分层结构——`model`（JPA 实体 + 枚举状态机）→ `repository`（JpaRepository 派生查询）→ `service`（业务规则 + `@Transactional`）→ `dto`（请求/响应 record + 校验注解）→ `controller`（只做 HTTP 输入输出）→ `exception`（@RestControllerAdvice，400/404/409 → ProblemDetail）。

**运行命令与结果**：见下方「测试与验证」，28 个测试全部通过；设计细节见 [docs/project-proposal.md](../../project-proposal.md)。

## 2. 单体应用实现

**完成内容**：

- 模型：`Item`、`ItemStatus`、`TradeOrder`、`OrderStatus`（`model/` 包）；
- Service：`ItemService`（发布/列表/详情）、`OrderService`（下单/取消/成交/查询）；
- Controller：`ItemController`、`OrderController`，共 8 个接口（含查询与创建，均以 `/api/` 前缀）；
- 业务规则集中在 `OrderService.placeOrder/cancel/complete`，Controller 不做业务判断、不直接访问数据。

**关键接口**（详见根目录 README 的接口表）：

| 接口 | 方法 | 成功 | 关键失败 |
| --- | --- | --- | --- |
| `/api/items` | POST / GET | 201 / 200 | 400（价格≤0） |
| `/api/items/{id}` | GET | 200 | 404 |
| `/api/orders` | POST | 201 | 404（商品不存在）/ 409（非在售、买自己、重复下单） |
| `/api/orders/{id}/cancel` `/complete` | POST | 200 | 404 / 409（订单已结束） |

**验证方法**：`./mvnw test`（Windows 用 `.\mvnw.cmd test`）+ curl 实测，截图见 `screenshots/01`、`03`。

## 3. 数据库持久化

**完成内容**：

- `docs/database.md`：两张业务表 `items`、`trade_orders` 的字段、主键与非空约束设计，以及「一单不可多卖、价格大于 0、状态单向流转」等约束的保证方式；
- Repository：`repository/` 包下 `ItemRepository`、`OrderRepository` 继承 `JpaRepository`，使用派生查询（`findByStatus`、`findByBuyerId`、`countByItemIdAndStatus` 等）；
- DTO：`dto/` 包下 `CreateItemRequest`、`CreateOrderRequest` 等 record，承载全部 API 输入并做 `@NotBlank`/`@DecimalMin` 校验，模型不直接暴露给 API；
- 测试数据准备：`src/test/resources/application.yml` 配 H2 内存库（`create-drop`），各测试类在 `@BeforeEach` 写入种子数据，可重复执行；
- MySQL 接入：`application.yml` 通过 `DB_URL/DB_USER/DB_PASSWORD` 环境变量连接，建库建用户语句与启动步骤见根目录 README「准备本地 MySQL」。

**验证方法**：`@DataJpaTest` 验证实体保存、状态过滤查询、按买家查询与进行中订单计数；集成测试验证真实 JPA 读写下的完整业务流。截图见 `screenshots/02`。

## 4. 测试与验证

**覆盖情况**（`cd monolith && ./mvnw test`，共 28 个，全部通过，可重复执行）：

| 类别 | 测试类 | 验证点 |
| --- | --- | --- |
| Repository 读写 | `ItemRepositoryTest` | 实体保存、按主键查询、按状态过滤 |
| Repository 读写 | `OrderRepositoryTest` | 订单保存、按买家查询、进行中订单计数 |
| Service 业务规则 | `OrderServiceTest`（Mockito 单测） | 下单成功锁定商品 / 商品不存在 404 规则 / 非在售 409 / 买自己 409 / 重复下单 409 / 取消恢复在售 / 终态订单不可取消 / 成交后商品售出 |
| Service 业务规则 | `OrderServiceIntegrationTest`（真实 JPA + H2） | 完整流程：下单→锁定→取消→再下单→成交→售出，及终态拒绝 |
| API 正常/错误响应 | `ItemControllerTest` | 201+Location / 400 参数校验 / 200 列表 / 404 |
| API 正常/错误响应 | `OrderControllerTest` | 201 / 409 / 404 / 400 / 取消与成交 200 / 买家列表 200 |
| 上下文 | `CampustradeMonolithApplicationTests` | 应用上下文加载 |

**运行命令**：`cd monolith && ./mvnw test`（Windows：`.\mvnw.cmd test`），实测两次均 `Tests run: 28, Failures: 0, Errors: 0` + `BUILD SUCCESS`。

**截图索引**（`screenshots/`）：

| 文件 | 内容 |
| --- | --- |
| `01-code-order-service.png` | 核心代码：`OrderService`（业务规则+事务）与 `OrderController`（接口） |
| `02-mvnw-test.png` | `mvnw test` 全部 28 个测试通过 |
| `03-api-run.png` | curl 实测：发布商品 201 → 下单 201 → 重复下单 409 → 取消 200 → 列表 |
