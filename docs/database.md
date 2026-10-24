# 数据库设计（Week 04）

本文档说明 CampusTrade 单体应用当前的两张业务表：`items`（商品）与 `trade_orders`（订单）。应用通过 Spring Data JPA 的 `ddl-auto: update` 在本地自动建表，表结构以本文档为准；正式环境的建表语句可据此导出。

## 表关系总览

```
items (1) ───< (N) trade_orders
  id  ◄──────────────  item_id（逻辑外键）
```

- 一个卖家（seller_id）可发布多个商品；
- 一个商品可产生多条历史订单（取消后再下单），但**任意时刻最多一条 CREATED 状态的进行中订单**（由 Service 规则 + `countByItemIdAndStatus` 查询保证，见下）；
- 订单通过 `item_id` / `buyer_id` 做逻辑关联，不建物理外键，便于后续微服务拆分后订单库与商品库独立。

## 表一：items（商品表）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | VARCHAR(36) | **主键** | UUID 字符串，应用生成 |
| seller_id | VARCHAR(36) | NOT NULL | 卖家用户 id |
| title | VARCHAR(100) | NOT NULL | 标题 |
| description | VARCHAR(1000) | 可空 | 描述 |
| price | DECIMAL(10,2) | NOT NULL | 售价，必须大于 0（DTO `@DecimalMin("0.01")` + 实体构造校验，应用层约束） |
| status | VARCHAR(20) | NOT NULL | ON_SALE / RESERVED / SOLD，字符串存储 |
| created_at | DATETIME | NOT NULL | 发布时间 |

说明：当前没有适合建唯一约束的自然字段（卖家允许发布多个同名商品），主键即唯一标识；价格正数约束放在应用层校验，因为 H2/MySQL 对 CHECK 约束的支持不一致。

## 表二：trade_orders（订单表）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | VARCHAR(36) | **主键** | UUID 字符串，应用生成 |
| item_id | VARCHAR(36) | NOT NULL | 商品 id，逻辑外键 → items.id |
| buyer_id | VARCHAR(36) | NOT NULL | 买家用户 id |
| amount | DECIMAL(10,2) | NOT NULL | 成交金额快照（下单时复制商品价格） |
| status | VARCHAR(20) | NOT NULL | CREATED / COMPLETED / CANCELLED |
| created_at | DATETIME | NOT NULL | 下单时间 |

表名用 `trade_orders` 而非 `orders`，避免与 SQL 关键字 ORDER 冲突。

## 必要约束与保证方式

| 约束 | 保证方式 |
| --- | --- |
| 主键唯一 | JPA `@Id`，UUID 应用生成 |
| 关键字段非空 | JPA `@Column(nullable = false)`，建表即约束 |
| 一单不可多卖 | Service 事务内先 `countByItemIdAndStatus(itemId, CREATED)` 判重，再写订单与商品状态，两次写入同事务（`@Transactional`） |
| 价格必须大于 0 | 请求 DTO `@DecimalMin("0.01")`（400）+ Item 构造器校验（IllegalArgumentException） |
| 订单状态只能向前流转 | Service 中取消/成交前检查 `status == CREATED`，否则 409 |
| 买家不能买自己的商品 | Service 中比较 `item.sellerId == buyerId`，冲突 409 |

## 数据访问与配置

- Repository 位于 `monolith/src/main/java/com/zjgsu/linkunze/repository/`，`ItemRepository` / `OrderRepository` 均继承 `JpaRepository`，派生查询方法见接口注释；
- 连接配置在 `monolith/src/main/resources/application.yml`，使用环境变量 `DB_URL` / `DB_USER` / `DB_PASSWORD`（默认指向本机 MySQL 的 `campustrade` 库）；
- 测试使用 H2 内存库（`src/test/resources/application.yml`，`create-drop`），测试数据由测试代码在 `@BeforeEach` 准备，可重复执行，见「测试与验证」一节。
