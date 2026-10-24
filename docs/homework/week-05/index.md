# Week 05：Docker 容器化

> **课程**：微服务开发与实践
> **本周目标**：将 Week 04 完成的单体应用与 MySQL 数据库容器化，实现可重复的镜像构建、一键启动、业务 API 验证和数据持久化。

## 1. Dockerfile 与镜像构建

### 实现思路

`monolith/Dockerfile` 采用**多阶段构建**：

- **构建阶段**（`maven:3.9-eclipse-temurin-25`）：在容器内完成依赖解析与 `mvn package`，本机无需安装 JDK/Maven；先拷贝 `pom.xml` 再拷贝源码，利用镜像层缓存加速重复构建。`settings.xml` 配置阿里云镜像加速依赖下载。
- **运行阶段**（`eclipse-temurin:25-jre`）：只从构建阶段拷贝可运行 JAR（约几十 MB），源码、Maven 缓存与构建工具都不进入最终镜像，体积小、攻击面小。

`monolith/.dockerignore` 排除了 `target/` 本地构建产物、IDE 配置（`.idea/`、`*.iml`）、日志与版本控制文件，保证镜像构建上下文干净。

### 执行命令（仓库根目录）

```bash
docker build -t campustrade:week05 ./monolith
docker images | findstr campustrade        # Windows；Linux/macOS 用 grep
```

### 结果

镜像 `campustrade:week05` 构建成功，多阶段构建的最终镜像仅包含 JRE 与可运行 JAR。

- 截图：[01-docker-build.png](screenshots/01-docker-build.png)（含 `docker images` 查看结果）

## 2. Compose 编排与启动

### 实现思路

根目录 `compose.yaml`（不使用已废弃的 `version` 字段）包含两个服务：

- **mysql**：官方镜像 `mysql:8.4.11`；命名数据卷 `mysql-data` 挂载到 `/var/lib/mysql`；`healthcheck` 用 `mysqladmin ping` 每 5 秒探测一次；本机端口映射 `3308:3306`（避开本机 MySQL 占用的 3306，也方便从本机排查）。
- **app**：构建上下文 `./monolith`，镜像即第 1 题构建的 `campustrade:week05`；通过**数据库服务名** `mysql` 连接（`jdbc:mysql://mysql:3306/campustrade`），不依赖本机端口；`depends_on` 配合 `condition: service_healthy` 保证应用在数据库健康后才启动；两个服务共享 `campustrade-net` 网络。

连接地址、账号、密码全部来自环境变量：`cp .env.example .env` 后填真实密码即可。`.env` 已加入 `.gitignore`，不会提交真实密码。

### 执行命令（仓库根目录）

```bash
cp .env.example .env          # 填写 .env 中的 DB_PASSWORD 与 MYSQL_ROOT_PASSWORD
docker compose up -d --build  # 一键构建并启动两个服务
docker compose ps             # 查看状态
```

### 结果

`campustrade-app` 与 `campustrade-mysql` 均成功启动，`campustrade-mysql` 状态为 `(healthy)`。

- 截图：[02-compose-ps.png](screenshots/02-compose-ps.png)

## 3. 个人业务 API 验证

通过容器映射到本机的 8080 端口验证健康接口与业务接口（一次创建 + 一次查询）：

```bash
# 健康检查
curl -i http://localhost:8080/actuator/health

# 创建商品（POST /api/items，201）
curl -i -X POST http://localhost:8080/api/items \
  -H 'Content-Type: application/json' \
  -d '{"sellerId":"u-seller","title":"九成新自行车","description":"毕业转让","price":120.00}'

# 查询在售商品列表（GET /api/items，200）
curl -i http://localhost:8080/api/items
```

### 结果

健康接口返回 `{"status":"UP"}`；创建商品返回 201 与新商品 id；列表查询返回 200 且包含刚创建的商品，容器化前后行为一致。

- 截图：[03-api-test.png](screenshots/03-api-test.png)

## 4. 数据持久化验证

### 验证方法

1. 用业务接口创建一条数据（商品），记录返回的 id 与关键字段；
2. 执行 `docker compose down` 删除容器（**不使用 `-v`**，命名数据卷 `mysql-data` 保留）；
3. 再执行 `docker compose up -d` 重建容器，等待应用启动；
4. 用同一 id 查询 `GET /api/items/{id}`，确认数据存在且 id、标题、价格等关键字段不变。

### 结果

容器删除重建后，`GET /api/items/{id}` 仍返回重建前创建的商品，id 与关键字段完全一致。

**命名数据卷的作用**：数据实际存储在 Docker 管理的卷 `mysql-data` 中，与容器生命周期解耦——容器可以随便删除重建，只要不用 `down -v` 删除卷，MySQL 数据就一直在。

- 截图：[04-persistence.png](screenshots/04-persistence.png)
